package nu.mine.mosher.genealogy;

import java.sql.*;
import java.util.*;

public class RbacAuthorizer {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<RbacSubject> subject;

    public RbacAuthorizer(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") final Optional<RbacSubject> subject) {
        this.subject = subject;
    }

    public void register() throws SQLException {
        if (this.subject.isEmpty()) {
            return;
        }

        int c = 0;
        try (
            final var connection = DatabaseHandler.connect();
            final var statement = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE email = ? or gid = ?")
        ) {
            statement.setString(1, this.subject.get().email());
            statement.setString(2, this.subject.get().gid());
            try (final var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    c = resultSet.getInt(1);
                }
            }
        }

        if (0 < c) {
            // TODO update missing gid or email
        } else {
            try (
                final var connection = DatabaseHandler.connect();
                final var statement = connection.prepareStatement("INSERT INTO users (email, gid) VALUES (?, ?)")
            ) {
                statement.setString(1, this.subject.get().email());
                statement.setString(2, this.subject.get().gid());
                statement.executeUpdate();
            }
        }
    }

    public boolean can(final RbacPermission permission) throws SQLException {
        return allPermissions().contains(permission);
    }

    public HashSet<RbacPermission> allPermissions() throws SQLException {
        final var allowed = new HashSet<RbacPermission>();

        if (this.subject.isEmpty()) {
            try (
                final var connection = DatabaseHandler.connect();
                final var statement = connection.prepareStatement("SELECT * FROM all_unauthenticated_permission_names")
            ) {
                try (final var resultSet = statement.executeQuery()) {
                    addPermssions(resultSet, allowed);
                }
            }
        } else {
            try (
                final var connection = DatabaseHandler.connect();
                final var statement = connection.prepareStatement("SELECT * FROM all_authenticated_permission_names WHERE email = ? or gid = ?")
            ) {
                statement.setString(1, this.subject.get().email());
                statement.setString(2, this.subject.get().gid());
                try (final var resultSet = statement.executeQuery()) {
                    addPermssions(resultSet, allowed);
                }
            }
        }
        return allowed;
    }

    public String display() {
        return this.subject.isPresent() ? this.subject.get().email() : "guest";
    }

    public boolean authenticated() {
        return this.subject.isPresent();
    }

    private void addPermssions(final ResultSet resultSet, final Collection<RbacPermission> permissions) throws SQLException {
        while (resultSet.next()) {
            final var p = resultSet.getString(1);
            if (!resultSet.wasNull()) {
                permissions.add(RbacPermission.valueOf(p));
            }
        }
    }
}
