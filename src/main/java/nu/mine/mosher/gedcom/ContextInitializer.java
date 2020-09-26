package nu.mine.mosher.gedcom;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;


@WebListener
public class ContextInitializer implements ServletContextListener {
    public static final String SQL_SESSION_FACTORY = "nu.mine.mosher.ftmviewer.SqlSessionFactory";

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        sce.getServletContext().setAttribute(SQL_SESSION_FACTORY, AppInitializer.init());
    }
}
