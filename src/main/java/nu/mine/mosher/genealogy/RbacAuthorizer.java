package nu.mine.mosher.genealogy;

import java.sql.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RbacAuthorizer {
    private final int id;
    private final String email;

    public RbacAuthorizer(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") final Optional<RbacSubject> subject, final ZonedDateTime now) throws SQLException {
        if (subject.isEmpty()) {
            this.id = 0;
            this.email = "guest";
        } else {
            /*
            search for gid in users (will be zero or one), if found
                search for email in emails (will be zero or one), if found
                    if id does not match
                        this.id = 0
                    else
                        this.id = users.id
                else
                    insert email for user
                    this.id = users.id
            else
                search for email in emails (will be zero or one), if found
                    if users.gid is not empty
                        this.id = 0
                    else
                        set users.gid
                        this.id = users.id
                else
                    insert user, email
                    this.id = users.id
             */
            final var idUserOfGid = findUserByGid(subject.get().gid());
            final var idUserOfEmail = findUserByEmail(subject.get().email());
            if (0 < idUserOfGid) {
                // Google ID was here previously
                if (0 < idUserOfEmail) {
                    if (idUserOfEmail != idUserOfGid) {
                        // requested email was here previously but under a different Google ID
                        this.id = 0;
                        this.email = "guest";
                    } else {
                        // requested email and Google ID match and were here previously
                        this.id = idUserOfGid;
                        this.email = subject.get().email();
                    }
                } else {
                    // Google ID has a new email associated with it
                    insertEmail(subject.get().email(), idUserOfGid, now);
                    this.id = idUserOfGid;
                    this.email = subject.get().email();
                }
            } else {
                // never-before seen Google ID
                if (0 < idUserOfEmail) {
                    // email was here before (we don't have a record of their Google ID)
                    String gid = fetchUserGid(idUserOfEmail);
                    if (!gid.isEmpty()) {
                        // email is already attached to a different Google ID
                        this.id = 0;
                        this.email = "guest";
                    } else {
                        // user record attached to email has no gid, so update it now
                        updateUserGid(idUserOfEmail, gid);
                        this.id = idUserOfEmail;
                        this.email = subject.get().email();
                    }
                } else {
                    // never-before seen email, so add a new user record and associated email
                    this.id = insertUser(subject.get().gid());
                    insertEmail(subject.get().email(), this.id, now);
                    this.email = subject.get().email();
                }
            }
        }
    }

    private static int insertUser(final String gid) throws SQLException {
        try (
            final var connection = DatabaseHandler.connect();
            final var statement = connection.prepareStatement("INSERT INTO users (gid) VALUES (?)", Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setString(1, gid);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
                else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        }
    }

    private static void updateUserGid(final int idUser, final String gid) throws SQLException {
        try (
            final var connection = DatabaseHandler.connect();
            final var statement = connection.prepareStatement("UPDATE users SET gid = ? WHERE id = ?")
        ) {
            statement.setString(1, gid);
            statement.setInt(2, idUser);
            statement.executeUpdate();
        }
    }

    private static String fetchUserGid(int idUser) throws SQLException {
        try (
            final var connection = DatabaseHandler.connect();
            final var statement = connection.prepareStatement("SELECT gid FROM users WHERE id = ?")
        ) {
            statement.setInt(1, idUser);
            try (final var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        return "";
    }

    private static void insertEmail(final String email, final int idUser, final ZonedDateTime now) throws SQLException {
        try (
            final var connection = DatabaseHandler.connect();
            final var statement = connection.prepareStatement("INSERT INTO emails (email, created, user) VALUES (?, ?, ?)")
        ) {
            statement.setString(1, email);
            statement.setString(2, now.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
            statement.setInt(3, idUser);
            statement.executeUpdate();
        }
    }

    private static int findUserByGid(final String gid) throws SQLException {
        try (
            final var connection = DatabaseHandler.connect();
            final var statement = connection.prepareStatement("SELECT id FROM users WHERE gid = ?")
        ) {
            statement.setString(1, gid);
            try (final var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return 0;
    }

    private static int findUserByEmail(final String email) throws SQLException {
        try (
            final var connection = DatabaseHandler.connect();
            final var statement = connection.prepareStatement("SELECT user FROM emails WHERE email = ?")
        ) {
            statement.setString(1, email);
            try (final var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return 0;
    }

    public boolean can(final RbacPermission permission) throws SQLException {
        return allPermissions().contains(permission);
    }

    public HashSet<RbacPermission> allPermissions() throws SQLException {
        final var allowed = new HashSet<RbacPermission>();

        if (authenticated()) {
            try (
                final var connection = DatabaseHandler.connect();
                final var statement = connection.prepareStatement("SELECT name FROM all_authenticated_permission_names WHERE user = ?")
            ) {
                statement.setInt(1, this.id);
                addPermssions(statement, allowed);
            }
        } else {
            try (
                final var connection = DatabaseHandler.connect();
                final var statement = connection.prepareStatement("SELECT name FROM all_unauthenticated_permission_names")
            ) {
                addPermssions(statement, allowed);
            }
        }
        return allowed;
    }

    public int userID() throws SQLException {
        return this.id;
    }

    public String display() {
        return this.email;
    }

    public boolean authenticated() {
        return 0 < this.id;
    }

    private static void addPermssions(final PreparedStatement statement, final Collection<RbacPermission> permissions) throws SQLException {
        try (final var resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                final var p = resultSet.getString(1);
                if (!resultSet.wasNull()) {
                    permissions.add(RbacPermission.valueOf(p));
                }
            }
        }
    }
}
