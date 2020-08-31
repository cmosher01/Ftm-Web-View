package nu.mine.mosher.gedcom;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import org.apache.ibatis.session.*;
import org.slf4j.*;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.sql.*;
import java.util.Optional;


@WebListener
public class ContextInitializer implements ServletContextListener {
    public static final String SQL_SESSION_FACTORY = "nu.mine.mosher.ftmviewer.SqlSessionFactory";



    private static final Logger LOG;

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.FINEST);
        LOG = LoggerFactory.getLogger(ContextInitializer.class);
        LOG.trace("ContextInitializer static initialization of logging framework complete");
    }

    private static final String CLASS_DRIVER_JDBC = "org.sqlite.JDBC";



    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        LOG.trace("entering ContextInitializer.contextInitialized()");

        try {
            initJdbc();
        } catch (final Throwable e) {
            throw new IllegalStateException("Error trying to load JDBC driver: "+CLASS_DRIVER_JDBC, e);
        }

        initMybatis(sce.getServletContext());

        LOG.trace("exiting ContextInitializer.contextInitialized()");
    }

    private void initMybatis(final ServletContext context) {
        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final Configuration config = new Configuration();
        initMybatisConfig(config);
        context.setAttribute(SQL_SESSION_FACTORY, builder.build(config));
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
        config.getTypeHandlerRegistry().register(FtmGuidTypeHandler.class);
        config.getTypeHandlerRegistry().register(RefnTypeHandler.class);

        config.addMapper(RefnMap.class);
        config.addMapper(RefnReverseMap.class);
        config.addMapper(PersonIndexMap.class);
        config.addMapper(PersonMap.class);
        config.addMapper(ParentsMap.class);
        config.addMapper(PartnershipsMap.class);
        config.addMapper(ChildrenMap.class);
    }
}
