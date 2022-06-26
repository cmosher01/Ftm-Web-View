package nu.mine.mosher.genealogy;

import liquibase.*;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.ui.LoggerUIService;
import org.slf4j.*;

import java.io.File;
import java.sql.*;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;



public class DatabaseHandler
{
    private static final Logger log =  LoggerFactory.getLogger(DatabaseHandler.class);
    private static final String LIQUIBASE_CHANGELOG_PATH = "/nu/mine/mosher/genealogy/liquibase/changelog.sql";




    public void liquibaseUpdate() throws LiquibaseException
    {
        liquibaseUpdate(Map.of(Scope.Attr.ui.name(), new LoggerUIService()));
    }


    private void liquibaseUpdate(final Map<String, Object> config) throws LiquibaseException
    {
        try
        {
            Scope.child(config, this::tryLiquibaseUpdate);
        }
        catch (final LiquibaseException e)
        {
            throw e;
        }
        catch (final Exception e)
        {
            throw new LiquibaseException(e);
        }
    }

    private void tryLiquibaseUpdate() throws SQLException, LiquibaseException
    {
        try (
            final var connection = connect();
            final var liquibase = liquibase(connection))
        {
            liquibase.update(new Contexts(), new LabelExpression());
        }
    }

    private static Liquibase liquibase(final Connection connection) throws LiquibaseException
    {
        return new Liquibase(LIQUIBASE_CHANGELOG_PATH, new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
    }

    public static Connection connect() throws SQLException
    {
        return DriverManager.getConnection("jdbc:sqlite:ftmwebview.sqlite");
    }

    public static void logRequest(final Instant ts, final String ip, final String agent, final String uri, final Optional<UUID> person, final Optional<IndexedDatabase> tree, final int user) throws SQLException {
        try (
            final var connection = DatabaseHandler.connect();
            final var statement = connection.prepareStatement(
                "INSERT INTO requests (ts,ip,agent,uri,person,tree,user) VALUES (?, ?, ?, ?, ?, ?, ?)")
        ) {
            statement.setString(1, ts.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
            statement.setString(2, ip);
            statement.setString(3, agent);
            statement.setString(4, uri);
            if (person.isEmpty()) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(5, person.get().toString());
            }
            if (tree.isEmpty()) {
                statement.setNull(6, Types.VARCHAR);
            } else {
                statement.setString(6, tree.get().file().getPath());
            }
            if (user <= 0) {
                statement.setNull(7, Types.INTEGER);
            } else {
                statement.setInt(7, user);
            }
            statement.executeUpdate();
        }
    }
}
