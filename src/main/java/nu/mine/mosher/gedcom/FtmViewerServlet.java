package nu.mine.mosher.gedcom;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.apache.ibatis.session.*;
import org.slf4j.*;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.w3c.dom.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class FtmViewerServlet extends HttpServlet {
    private static final Logger LOG;
    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.FINEST);
        LOG = LoggerFactory.getLogger(FtmViewerServlet.class);
    }

    private static final String XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml";

    private final SqlSessionFactory factory;

    public FtmViewerServlet() throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");

        final SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        final Configuration config = new Configuration();
        addMapsTo(config);
        this.factory = builder.build(config);
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException {
        try {
            LOG.trace("GET {}", req.getRequestURI());
            tryGet(req, resp);
        } catch (final Throwable e) {
            LOG.error("uncaught exception in servlet", e);
        }
    }

    private void tryGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException, ParserConfigurationException, TransformerException, SQLException {
        LOG.debug("requestURL={}", req.getRequestURL());
        LOG.debug("requestURI={}", req.getRequestURI());
        LOG.debug("pathInfo={}", req.getPathInfo());
        LOG.debug("servletPath={}", req.getServletPath());
        final Enumeration<String> e = req.getHeaderNames();
        while (e.hasMoreElements()) {
            final String header = e.nextElement();
            LOG.debug("header: {}={}", header, req.getHeader(header));
        }



        Optional<Document> dom = Optional.empty();

        if (req.getServletPath().equals("/")) {
            final Optional<UUID> uuidPerson = getRequestedPerson(req);
            if (uuidPerson.isPresent()) {
                LOG.debug("person_uuid: {}", uuidPerson.get());
            } else {
                LOG.debug("no valid 'person_uuid' query parameter found");
            }

            final Optional<String> nameTree = getRequestedTree(req);
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
                            redirectToTreePerson(indexedDatabaseOther.get(), uuidPerson.get(), resp);
                        } else {
                            redirectToTree(indexedDatabase.get(), resp);
                        }
                    }
                } else {
                    final Optional<IndexedDatabase> indexedDatabaseOther = findPersonInAnyTree(uuidPerson.get());
                    if (indexedDatabaseOther.isPresent()) {
                        redirectToTreePerson(indexedDatabaseOther.get(), uuidPerson.get(), resp);
                    } else {
                        LOG.info("person not currently found in any tree");
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }
                }
            } else if (nameTree.isPresent()) {
                if (indexedDatabase.isPresent()) {
                    dom = Optional.of(pageIndexPeople(indexedDatabase.get()));
                } else {
                    LOG.info("tree does not currently exist");
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            } else if (uuidPerson.isPresent()) {
                final Optional<IndexedDatabase> indexedDatabaseOther = findPersonInAnyTree(uuidPerson.get());
                if (indexedDatabaseOther.isPresent()) {
                    redirectToTreePerson(indexedDatabaseOther.get(), uuidPerson.get(), resp);
                } else {
                    LOG.info("person not currently found in any tree");
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            } else {
                dom = Optional.of(pageIndexDatabases());
            }
        } else {
            LOG.warn("Unexpected servlet path: {}", req.getServletPath());
            LOG.info("requested resource not found");
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }




        if (dom.isPresent()) {
            resp.setContentType("application/xhtml+xml; charset=utf-8");
            final BufferedOutputStream out = new BufferedOutputStream(resp.getOutputStream());
            serialize(dom.get(), out, true, true);
            out.flush();
            out.close();
        }
    }

    private void redirectToTree(final IndexedDatabase idb, final HttpServletResponse response) {
        final String location = "?"+URLEncodedUtils.format(
            List.of(
                new BasicNameValuePair("tree", idb.file().getName())),
            StandardCharsets.UTF_8);
        response.setHeader("Location", location);
        LOG.info("redirecting to: {}", location);
        response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
    }


    private static void redirectToTreePerson(final IndexedDatabase idb, final UUID uuidPerson, final HttpServletResponse response) {
        final String location = "?"+URLEncodedUtils.format(
            List.of(
                new BasicNameValuePair("tree", idb.file().getName()),
                new BasicNameValuePair("person_uuid", uuidPerson.toString())),
            StandardCharsets.UTF_8);
        response.setHeader("Location", location);
        LOG.info("redirecting to: {}", location);
        response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
    }

    private static Optional<IndexedDatabase> findPersonInAnyTree(final UUID uuid) {
        return Optional.empty();
    }

    private static Optional<IndexedPerson> findPersonInTree(final IndexedDatabase idb, final UUID uuid) {
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
        final Document dom = empty();

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

    private Document pageIndexPeople(final IndexedDatabase iDb) throws ParserConfigurationException, SQLException {

        List<IndexedPerson> list = Collections.emptyList();
        try (final Connection conn = DriverManager.getConnection("jdbc:sqlite:"+iDb.file())) {
            LOG.debug("database connection: auto-commit={}, transaction-isolation={}", conn.getAutoCommit(), conn.getTransactionIsolation());
            try (final SqlSession session = this.factory.openSession(conn)) {
                final PersonIndexMap map = session.getMapper(PersonIndexMap.class);
                list = map.select();
                list.forEach(p -> LOG.debug("person: {}", p));
            }
        }



        final Document dom = empty();

        final Element html = e(dom, "html");
        final Element head = e(html, "head");
        final Element body = e(html, "body");

        final Element h1 = e(body, "h3");
        h1.setTextContent(iDb.toString());

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

    private static Document pagePerson(IndexedDatabase idb, IndexedPerson ipr) throws ParserConfigurationException {
        final Document dom = empty();

        final Element html = e(dom, "html");
        final Element head = e(html, "head");
        final Element body = e(html, "body");

        return dom;
    }



    private static void addMapsTo(final Configuration config) {
        config.addMapper(PersonIndexMap.class);
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







    private static Element e(final Node parent, final String tag) {
        final Document dom;
        if (parent instanceof Document) {
            dom = (Document)parent;
        } else {
            dom = parent.getOwnerDocument();
        }
        final Element element = dom.createElementNS(XHTML_NAMESPACE, tag);
        parent.appendChild(element);
        return element;
    }

    public static Document empty() throws ParserConfigurationException {
        return factory(false, Collections.emptyList()).newDocumentBuilder().newDocument();
    }

    private static DocumentBuilderFactory factory(final boolean validate, final List<URL> schemas) throws ParserConfigurationException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setValidating(validate);
        factory.setFeature("http://apache.org/xml/features/validation/schema", validate);

        factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(false);
        factory.setIgnoringComments(false);

        factory.setFeature("http://apache.org/xml/features/honour-all-schemaLocations", true);
        factory.setFeature("http://apache.org/xml/features/warn-on-duplicate-entitydef", true);
        factory.setFeature("http://apache.org/xml/features/standard-uri-conformant", true);
        factory.setFeature("http://apache.org/xml/features/xinclude", true);
        factory.setFeature("http://apache.org/xml/features/validate-annotations", true);
        factory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
        factory.setFeature("http://apache.org/xml/features/validation/warn-on-duplicate-attdef", true);
        factory.setFeature("http://apache.org/xml/features/validation/warn-on-undeclared-elemdef", true);
        factory.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);

        //These options often crash Xerces (as of 2.12.0):
        //factory.setFeature("http://apache.org/xml/features/scanner/notify-char-refs", true);
        //factory.setFeature("http://apache.org/xml/features/scanner/notify-builtin-refs", true);

        factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", XMLConstants.W3C_XML_SCHEMA_NS_URI);

        if (!schemas.isEmpty()) {
            factory.setAttribute(
                "http://java.sun.com/xml/jaxp/properties/schemaSource",
                schemas.stream().sequential().map(URL::toExternalForm).toArray(String[]::new));
        }

        return factory;
    }

    public static void serialize(final Node dom, final BufferedOutputStream to, final boolean pretty, final boolean xmldecl) throws IOException, TransformerException {
        final DOMSource source = new DOMSource(dom, dom.getBaseURI());
        final StreamResult result = new StreamResult(to);
        result.setSystemId(dom.getBaseURI());
        final Transformer transformIdentity = TransformerFactory.newInstance().newTransformer();
        configTransformer(transformIdentity, pretty, xmldecl);
        transformIdentity.transform(source, result);
        to.flush();
    }

    private static void configTransformer(final Transformer transform, final boolean pretty, final boolean xmldecl) {
        transform.setOutputProperty(OutputKeys.METHOD, "xml");
        transform.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        transform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, xmldecl ? "no" : "yes");

        if (pretty) {
            transform.setOutputProperty(OutputKeys.INDENT, "yes");
            transform.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        }
    }
}
