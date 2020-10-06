package nu.mine.mosher.gedcom;

import org.apache.ibatis.session.*;
import org.slf4j.*;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class AppInitializer {
    private static final Logger LOG;

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.FINEST);
        LOG = LoggerFactory.getLogger(AppInitializer.class);
        LOG.trace("AppInitializer static initialization of logging framework complete");
    }

    private static final String CLASS_DRIVER_JDBC = "org.sqlite.JDBC";

    public static SqlSessionFactory init() {
        try {
            initJdbc();
        } catch (final Throwable e) {
            throw new IllegalStateException("Error trying to load JDBC driver: "+CLASS_DRIVER_JDBC, e);
        }
        return initMybatis();
    }

    private static SqlSessionFactory initMybatis() {
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final Configuration config = new Configuration();
        initMybatisConfig(config);
        return builder.build(config);
    }

    private static void initJdbc() throws ClassNotFoundException, SQLException {
        LOG.trace("loading JDBC driver: {}...", CLASS_DRIVER_JDBC);
        LOG.info("successfully loaded JDBC driver class: {}", Class.forName(CLASS_DRIVER_JDBC).getCanonicalName());

        final Driver driverJdbc = DriverManager.getDriver("jdbc:sqlite:");
        LOG.info("JDBC driver version: major={},minor={}", driverJdbc.getMajorVersion(), driverJdbc.getMinorVersion());

        final Optional<java.util.logging.Logger> jdbcLogger = Optional.ofNullable(driverJdbc.getParentLogger());
        if (jdbcLogger.isPresent()) {
            jdbcLogger.get().info("Logging via JDBC driver logger: " + jdbcLogger.toString());
        } else {
            LOG.info("JDBC driver logger not found.");
        }

        // TODO log anything from this??? driverJdbc.getPropertyInfo();
    }

    private static void initMybatisConfig(final Configuration config) {
        config.setCallSettersOnNulls(true);

        config.getTypeHandlerRegistry().register(FtmGuidTypeHandler.class);
        config.getTypeHandlerRegistry().register(RefnTypeHandler.class);
        config.getTypeHandlerRegistry().register(FtmDateTypeHandler.class);
        config.getTypeHandlerRegistry().register(FtmPlaceTypeHandler.class);
        config.getTypeHandlerRegistry().register(FtmNatureTypeHandler.class);
        config.getTypeHandlerRegistry().register(FtmRelationshipStatusTypeHandler.class);

        config.addMapper(RefnMap.class);
        config.addMapper(RefnReverseMap.class);
        config.addMapper(PersonIndexMap.class);
        config.addMapper(PersonMap.class);
        config.addMapper(PersonDetailsMap.class);
        config.addMapper(ParentsMap.class);
        config.addMapper(PartnershipsMap.class);
        config.addMapper(ChildrenMap.class);
        config.addMapper(EventsMap.class);
        config.addMapper(EventSourcesMap.class);
        config.addMapper(NotesMap.class);
        config.addMapper(SourceMap.class);
    }






    // example main running outside tomcat, for easy debugging
    public static void main(String[] args) throws SQLException {
        final SqlSessionFactory sqlSessionFactory = AppInitializer.init();
        IndexedDatabase indexedDatabase = new IndexedDatabase(new File("example/Disosway.ftm"));
        try (final Connection conn = DriverManager.getConnection("jdbc:sqlite:"+indexedDatabase.file()); final SqlSession session = sqlSessionFactory.openSession(conn)) {
            FtmLink link = new FtmLink(FtmLinkTableID.Person, 29);
            Map<Integer, EventWithSources> mapEventSources =
                session.
                getMapper(EventSourcesMap.class).
                select(link).
                stream().
                collect(Collectors.toMap(e -> e.pkidFact, e -> e));
        }
    }
}
