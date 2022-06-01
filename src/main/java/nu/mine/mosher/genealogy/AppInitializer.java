package nu.mine.mosher.genealogy;

import org.apache.ibatis.session.*;
import org.slf4j.*;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class AppInitializer {
    private static final Logger LOG;

    static {
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
            jdbcLogger.get().info("Logging via JDBC driver logger: " + jdbcLogger);
        } else {
            LOG.info("JDBC driver logger not found.");
        }
    }

    private static void initMybatisConfig(final Configuration config) {
        config.setCallSettersOnNulls(true);
        config.setArgNameBasedConstructorAutoMapping(true);

        config.getTypeHandlerRegistry().register(FtmGuidTypeHandler.class);
        config.getTypeHandlerRegistry().register(RefnTypeHandler.class);
        config.getTypeHandlerRegistry().register(FtmDateTypeHandler.class);
        config.getTypeHandlerRegistry().register(FtmPlaceTypeHandler.class);
        config.getTypeHandlerRegistry().register(FtmNatureTypeHandler.class);
        config.getTypeHandlerRegistry().register(FtmRelationshipStatusTypeHandler.class);
        config.getTypeHandlerRegistry().register(FtmFactTypeTagTypeHandler.class);
        config.getTypeHandlerRegistry().register(TimestampTypeHandler.class);

        config.addMapper(RefnMap.class);
        config.addMapper(RefnReverseMap.class);
        config.addMapper(PersonIndexMap.class);
        config.addMapper(PersonMap.class);
        config.addMapper(PersonQuickMap.class);
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
        IndexedDatabase indexedDatabase = new IndexedDatabase(new File("example/mosher_other_REF.ftm"));
        try (final Connection conn = DriverManager.getConnection("jdbc:sqlite:"+indexedDatabase.file()); final SqlSession session = sqlSessionFactory.openSession(conn)) {
            FtmLink link = new FtmLink(FtmLinkTableID.Person, 238);
            Map<Integer, EventWithSources> mapEventSources =
                session.
                getMapper(EventSourcesMap.class).
                select(link).
                stream().
                collect(Collectors.toMap(e -> e.pkidFact, e -> e));
//            final var mapper = session.getMapper(PersonQuickMap.class);
//            final var uuid = "5ed3b4f4-ddd4-4df1-854f-c351271c841d";
//            final Long id = mapper.select(uuid);
//            if (Objects.isNull(id)) {
//                LOG.info("can't find uuid: {}", uuid);
//                return;
//            }
//            LOG.info("id: {}", id);
//
//            final var m1 = session.getMapper(PersonMap.class);
//            final var indexedPerson = m1.select(id);
//            LOG.info("indexed person: {}", indexedPerson.id());
        }
    }
}
