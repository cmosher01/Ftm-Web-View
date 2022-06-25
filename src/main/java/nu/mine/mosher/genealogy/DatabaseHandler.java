package nu.mine.mosher.genealogy;

import liquibase.*;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.ui.LoggerUIService;
import org.slf4j.*;

import java.sql.*;
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
}
