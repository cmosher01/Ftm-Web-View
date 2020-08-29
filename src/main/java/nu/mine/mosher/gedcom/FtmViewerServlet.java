package nu.mine.mosher.gedcom;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.apache.ibatis.session.*;
import org.slf4j.*;
import org.w3c.dom.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static nu.mine.mosher.gedcom.ContextInitializer.SQL_SESSION_FACTORY;
import static nu.mine.mosher.gedcom.XmlUtils.e;

public class FtmViewerServlet extends HttpServlet {
    private static final Logger LOG =  LoggerFactory.getLogger(FtmViewerServlet.class);

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            LOG.trace("GET {}", request.getRequestURI());
            tryGet(request, response);
        } catch (final Throwable e) {
            LOG.error("uncaught exception in servlet", e);
        }
    }

    private void tryGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ParserConfigurationException, TransformerException, SQLException {
        logRequestInfo(request);

        Optional<Document> dom = Optional.empty();

        if (request.getServletPath().equals("/")) {
            dom = handleRequest(request, response);
        } else {
            LOG.warn("Unexpected servlet path: {}", request.getServletPath());
            LOG.info("requested resource not found");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

        if (dom.isPresent()) {
            response.setContentType("application/xhtml+xml; charset=utf-8");
            final BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream());
            XmlUtils.serialize(dom.get(), out, true, true);
            out.flush();
            out.close();
        }
    }

    private static void logRequestInfo(final HttpServletRequest request) {
        LOG.debug("requestURL={}", request.getRequestURL());
        LOG.debug("requestURI={}", request.getRequestURI());
        LOG.debug("pathInfo={}", request.getPathInfo());
        LOG.debug("servletPath={}", request.getServletPath());
        final Enumeration<String> e = request.getHeaderNames();
        while (e.hasMoreElements()) {
            final String header = e.nextElement();
            LOG.debug("header: {}={}", header, request.getHeader(header));
        }
    }

    private Optional<Document> handleRequest(final HttpServletRequest request, final HttpServletResponse response) throws ParserConfigurationException, IOException, SQLException {
        Optional<Document> dom = Optional.empty();

        final Optional<UUID> uuidPerson = getRequestedPerson(request);
        if (uuidPerson.isPresent()) {
            LOG.debug("person_uuid: {}", uuidPerson.get());
        } else {
            LOG.debug("no valid 'person_uuid' query parameter found");
        }

        final Optional<String> nameTree = getRequestedTree(request);
        final Optional<IndexedDatabase> indexedDatabase;
        if (nameTree.isPresent()) {
            LOG.debug("tree: {}", nameTree.get());
            indexedDatabase = findTree(nameTree.get());
            if (indexedDatabase.isPresent()) {
                LOG.debug("tree found: {}", indexedDatabase.get().file());
            } else {
                LOG.warn("tree not found: {}", nameTree.get());
            }
        } else {
            LOG.debug("no valid 'tree' query parameter found");
            indexedDatabase = Optional.empty();
        }


        if (nameTree.isPresent() && uuidPerson.isPresent()) {
            if (indexedDatabase.isPresent()) {
                final Optional<IndexedPerson> indexedPerson = findPersonInTree(indexedDatabase.get(), uuidPerson.get());
                if (indexedPerson.isPresent()) {
                    dom = Optional.of(pagePerson(indexedDatabase.get(), indexedPerson.get()));
                } else {
                    final Optional<IndexedDatabase> indexedDatabaseOther = findPersonInAnyTree(uuidPerson.get());
                    if (indexedDatabaseOther.isPresent()) {
                        redirectToTreePerson(indexedDatabaseOther.get(), uuidPerson.get(), response);
                    } else {
                        redirectToTree(indexedDatabase.get(), response);
                    }
                }
            } else {
                final Optional<IndexedDatabase> indexedDatabaseOther = findPersonInAnyTree(uuidPerson.get());
                if (indexedDatabaseOther.isPresent()) {
                    redirectToTreePerson(indexedDatabaseOther.get(), uuidPerson.get(), response);
                } else {
                    LOG.info("person not currently found in any tree");
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        } else if (nameTree.isPresent()) {
            if (indexedDatabase.isPresent()) {
                dom = Optional.of(pageIndexPeople(indexedDatabase.get()));
            } else {
                LOG.info("tree does not currently exist");
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } else if (uuidPerson.isPresent()) {
            final Optional<IndexedDatabase> indexedDatabaseOther = findPersonInAnyTree(uuidPerson.get());
            if (indexedDatabaseOther.isPresent()) {
                redirectToTreePerson(indexedDatabaseOther.get(), uuidPerson.get(), response);
            } else {
                LOG.info("person not currently found in any tree");
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } else {
            dom = Optional.of(pageIndexDatabases());
        }
        return dom;
    }

    private static void redirectToTree(final IndexedDatabase indexedDatabase, final HttpServletResponse response) {
        final String location = "?"+URLEncodedUtils.format(
            List.of(
                new BasicNameValuePair("tree", indexedDatabase.file().getName())),
            StandardCharsets.UTF_8);
        response.setHeader("Location", location);
        LOG.info("redirecting to: {}", location);
        response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
    }


    private static void redirectToTreePerson(final IndexedDatabase indexedDatabase, final UUID uuidPerson, final HttpServletResponse response) {
        final String location = "?"+URLEncodedUtils.format(
            List.of(
                new BasicNameValuePair("tree", indexedDatabase.file().getName()),
                new BasicNameValuePair("person_uuid", uuidPerson.toString())),
            StandardCharsets.UTF_8);
        response.setHeader("Location", location);
        LOG.info("redirecting to: {}", location);
        response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
    }

    private static Optional<IndexedDatabase> findPersonInAnyTree(final UUID uuidPerson) {
        return Optional.empty();
    }

    private static Optional<IndexedPerson> findPersonInTree(final IndexedDatabase indexedDatabase, final UUID uuidPerson) {
        return Optional.empty();
    }

    private static Optional<IndexedDatabase> findTree(final String nameTree) {
        for (final IndexedDatabase idb : loadDatabaseIndex()) {
            if (idb.file().getName().equals(nameTree)) {
                return Optional.of(idb);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> getRequestedTree(final HttpServletRequest request) {
        final Optional<String> optNameTree = Optional.ofNullable(request.getParameter("tree"));
        if (optNameTree.isPresent() && !optNameTree.get().isBlank()) {
            return optNameTree;
        }
        return Optional.empty();
    }

    private static Optional<UUID> getRequestedPerson(final HttpServletRequest request) {
        final Optional<String> optStringUuidPerson = Optional.ofNullable(request.getParameter("person_uuid"));
        if (optStringUuidPerson.isPresent()) {
            try {
                return Optional.of(UUID.fromString(optStringUuidPerson.get()));
            } catch (final Throwable e) {
                LOG.warn("Invalid format for UUID on person_uuid query parameter.", e);
            }
        }
        return Optional.empty();
    }

    private static Document pageIndexDatabases() throws ParserConfigurationException {
        final Document dom = XmlUtils.empty();

        final Element html = e(dom, "html");
        final Element head = e(html, "head");
        final Element body = e(html, "body");

        final Element h1 = e(body, "h3");
        h1.setTextContent("Browse a genealogy database");

        final Element ul = e(body, "ul");
        for (final IndexedDatabase iDb : loadDatabaseIndex()) {
            LOG.debug("    {}", iDb.file());
            final Element li = e(ul, "li");
            final Element a = e(li, "a");
            a.setAttribute("href", "?tree="+iDb.file().getName());
            a.setTextContent("{"+iDb.file().getName()+"}");
        }

        return dom;
    }

    private Document pageIndexPeople(final IndexedDatabase indexedDatabase) throws ParserConfigurationException, SQLException {
        List<IndexedPerson> list = Collections.emptyList();
        try (final Connection conn = DriverManager.getConnection("jdbc:sqlite:"+indexedDatabase.file())) {
            LOG.debug("database connection: auto-commit={}, transaction-isolation={}", conn.getAutoCommit(), conn.getTransactionIsolation());
            final SqlSessionFactory sqlSessionFactory = (SqlSessionFactory)getServletContext().getAttribute(SQL_SESSION_FACTORY);
            try (final SqlSession session = sqlSessionFactory.openSession(conn)) {
                final PersonIndexMap map = session.getMapper(PersonIndexMap.class);
                list = map.select();
                list.forEach(p -> LOG.debug("person: {}", p));
            }
        }



        final Document dom = XmlUtils.empty();

        final Element html = e(dom, "html");
        final Element head = e(html, "head");
        final Element body = e(html, "body");

        final Element h1 = e(body, "h3");
        h1.setTextContent(indexedDatabase.toString());

        final Element a = e(body, "a");
        a.setAttribute("href", "./");
        a.setTextContent("{home}");

        final Element ul = e(body, "ul");
        for (final IndexedPerson iPerson : list) {
            final Element li = e(ul, "li");
            final Element ap = e(li, "a");
            ap.setAttribute("href", "?person_uuid=TODO");
            ap.setTextContent(iPerson.nameSort());
        }

        return dom;
    }

    private static Document pagePerson(IndexedDatabase indexedDatabase, IndexedPerson indexedPerson) throws ParserConfigurationException {
        final Document dom = XmlUtils.empty();

        final Element html = e(dom, "html");
        final Element head = e(html, "head");
        final Element body = e(html, "body");

        return dom;
    }







    private static List<IndexedDatabase> loadDatabaseIndex() {
        final String sdirDbs = Optional.ofNullable(System.getenv("ftm_dir")).orElse("/srv");
        final Path dirDbs = Path.of(sdirDbs).toAbsolutePath().normalize();
        LOG.debug("Loading FTM data from trees in this directory: {}", dirDbs);
        return Arrays.stream(
            dirDbs.
                toFile().
                listFiles(ftmDbFilter())).
            map(IndexedDatabase::new).
            collect(Collectors.toList());
    }

    private static FileFilter ftmDbFilter() {
        return f -> f.isFile() && f.canRead() && f.getName().toLowerCase().endsWith(".ftm");
    }
}
