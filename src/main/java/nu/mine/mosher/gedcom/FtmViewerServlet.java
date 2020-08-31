package nu.mine.mosher.gedcom;

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
        LOG.trace("REQUEST HANDLING: BEGIN {}", "=".repeat(50));
        try {
            LOG.trace("GET {}", request.getRequestURI());
            logRequestInfo(request);
            tryGet(request, response);
        } catch (final Throwable e) {
            LOG.error("uncaught exception in servlet", e);
        }
        LOG.trace("REQUEST HANDLING: END   {}", "=".repeat(50));
    }

    private void tryGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ParserConfigurationException, TransformerException, SQLException {
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
        LOG.debug("queryString={}", request.getQueryString());
        LOG.debug("remoteUser={}", request.getRemoteUser());

        final Enumeration<String> e = request.getHeaderNames();
        while (e.hasMoreElements()) {
            final String header = e.nextElement();
            LOG.debug("header: {}={}", header, request.getHeader(header));
        }
        final Cookie[] cookies = request.getCookies();
        if (Objects.isNull(cookies) || cookies.length <= 0) {
            LOG.debug("The request had no cookies attached.");
        } else {
            Arrays.stream(cookies).forEach(c -> LOG.debug("cookie: {}: {}={}", c.getPath(), c.getName(), c.getValue()));
        }
        final Map<String, String[]> parameters = request.getParameterMap();
        if (Objects.isNull(parameters) || parameters.isEmpty()) {
            LOG.debug("The request had no query parameters.");
        } else {
            parameters.entrySet().forEach(FtmViewerServlet::logQueryParams);
        }
    }

    private static void logQueryParams(Map.Entry<String, String[]> entry) {
        final String name = entry.getKey();
        final String[] values = entry.getValue();
        if (Objects.isNull(values)) {
            LOG.debug("query parameter: {}={}", name, "<<NULL>>");
        } else if (values.length <= 0) {
            LOG.debug("query parameter: {}={}", name, "<<EMPTY>>");
        } else {
            Arrays.stream(values).forEach(value -> LOG.debug("query parameter: {}={}", name, value));
        }
    }

    private Optional<Document> handleRequest(final HttpServletRequest request, final HttpServletResponse response) throws ParserConfigurationException, IOException, SQLException {
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



        Optional<Document> dom = Optional.empty();

        if (nameTree.isPresent() && uuidPerson.isPresent()) {
            if (indexedDatabase.isPresent()) {
                final Optional<UUID> uuidPersonFiltered = findPersonInTree(indexedDatabase.get(), uuidPerson.get());
                if (uuidPersonFiltered.isPresent()) {
                    dom = Optional.of(pagePerson(indexedDatabase.get(), uuidPersonFiltered.get()));
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
        final String location = urlQueryTree(indexedDatabase);
        response.setHeader("Location", location);
        LOG.info("redirecting to: {}", location);
        response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
    }


    private static void redirectToTreePerson(final IndexedDatabase indexedDatabase, final UUID uuidPerson, final HttpServletResponse response) {
        final String location = urlQueryTreePerson(indexedDatabase, uuidPerson);
        response.setHeader("Location", location);
        LOG.info("redirecting to: {}", location);
        response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
    }

    private static String urlQueryTreePerson(IndexedDatabase indexedDatabase, UUID uuidPerson) {
        return "?"+URLEncodedUtils.format(
            List.of(
                new BasicNameValuePair("tree", indexedDatabase.file().getName()),
                new BasicNameValuePair("person_uuid", uuidPerson.toString())),
            StandardCharsets.UTF_8);
    }

    private static String urlQueryTree(IndexedDatabase indexedDatabase) {
        return "?"+URLEncodedUtils.format(
            List.of(
                new BasicNameValuePair("tree", indexedDatabase.file().getName())),
            StandardCharsets.UTF_8);
    }

//    private static String urlQueryPerson(UUID uuidPerson) {
//        return "?"+URLEncodedUtils.format(
//            List.of(
//                new BasicNameValuePair("person_uuid", uuidPerson.toString())),
//            StandardCharsets.UTF_8);
//    }

    private static Optional<IndexedDatabase> findPersonInAnyTree(final UUID uuidPerson) {
        return Optional.empty();
    }

    private Optional<UUID> findPersonInTree(final IndexedDatabase indexedDatabase, final UUID uuidPerson) throws SQLException {
        final int c;
        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final PersonMap map = session.getMapper(PersonMap.class);
            c = map.count(IndexedPerson.from(uuidPerson));
            LOG.debug("PersonGUID={}, count of matching Person rows: {}", uuidPerson, c);
        }
        if (0 < c) {
            return Optional.of(uuidPerson);
        }

        // if can't find by PersonGUID, search by REFN/*UID tag
        final Optional<UUID> optUuidPK = findPersonInTreeByRefn(indexedDatabase, new Refn(uuidPerson));
        if (optUuidPK.isPresent()) {
            return optUuidPK;
        }

        return Optional.empty();
    }

    private Optional<UUID> findPersonInTreeByRefn(final IndexedDatabase indexedDatabase, final Refn uuidRefn) throws SQLException {
        final Optional<UUID> optPersonGuid;
        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final RefnMap map = session.getMapper(RefnMap.class);
            optPersonGuid = Optional.ofNullable(map.select(uuidRefn));
            if (optPersonGuid.isPresent()) {
                LOG.debug("located matching Person for REFN {}: PersonGUID={}", uuidRefn, optPersonGuid.get());
            } else {
                LOG.info("Did not find REFN {}", uuidRefn);
            }
        }
        return optPersonGuid;
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
            a.setAttribute("href", urlQueryTree(iDb));
            a.setTextContent("{"+iDb.file().getName()+"}");
        }

        return dom;
    }

    private Document pageIndexPeople(final IndexedDatabase indexedDatabase) throws ParserConfigurationException, SQLException {
        final List<IndexedPerson> list;
        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final PersonIndexMap map = session.getMapper(PersonIndexMap.class);
            list = map.select();
            list.sort(Comparator.naturalOrder());
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
        for (final IndexedPerson indexedPerson : list) {
            final Element li = e(ul, "li");
            final Element ap = e(li, "a");
            ap.setAttribute("href", urlQueryTreePerson(indexedDatabase, indexedPerson.preferRefn()));
            ap.setTextContent(indexedPerson.name());
        }

        return dom;
    }

    private SqlSession openSessionFor(final Connection connection) {
        final SqlSessionFactory sqlSessionFactory = (SqlSessionFactory)getServletContext().getAttribute(SQL_SESSION_FACTORY);
        return sqlSessionFactory.openSession(connection);
    }

    private Document pagePerson(IndexedDatabase indexedDatabase, UUID uuidPerson) throws ParserConfigurationException, SQLException {
        final Document dom = XmlUtils.empty();

        final Element html = e(dom, "html");
        final Element head = e(html, "head");
        final Element body = e(html, "body");

        final FullPerson person = fragPersonParents(indexedDatabase, uuidPerson, body);

        final Element header = e(body, "header");
        e(header, "hr");
        final Element h1 = e(header, "h1");
        h1.setAttribute("class", "personName");
        h1.setTextContent(person.toString());

        fragPersonPartnerships(indexedDatabase, uuidPerson, body);

        return dom;
    }

    private FullPerson fragPersonParents(IndexedDatabase indexedDatabase, UUID uuidPerson, Element body) throws SQLException {
        final FullPerson person;
        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final ParentsMap map = session.getMapper(ParentsMap.class);
            person = map.select(IndexedPerson.from(uuidPerson));
        }

        final List<PersonParent> parents = person.parents;
        LOG.debug("parents selected: {}", parents);

        final Element section = e(body, "section");
        section.setAttribute("class", "parents");
        e(section, "hr");
        if (parents.isEmpty()) {
            final Element table = e(section, "table");
            final Element tbody = e(table, "tbody");
            final Element tr = e(tbody, "tr");
            final Element td = e(tr, "td");
            final Element span = e(td, "span");
            span.setAttribute("class", "missing");
            span.setTextContent("[no known parents (in this database)]");
        } else {
            final Element table = e(section, "table");
            final Element tbody = e(table, "tbody");
            for (final PersonParent parent : parents) {
                if (Objects.isNull(parent.id())) {
                    LOG.warn("For parents, ignoring NULL entry.");
                } else {
                    UUID uuidLink= parent.id();
                    final Optional<Refn> optRefn = findRefnFor(indexedDatabase, uuidLink);
                    if (optRefn.isPresent()) {
                        uuidLink = optRefn.get().uuid();
                    }
                    final Element tr = e(tbody, "tr");
                    final Element td = e(tr, "td");
                    final Element nature = e(td, "span");
                    nature.setAttribute("class", "nature");
                    nature.setTextContent("(" + parent.nature() + ")");
                    final Element a = e(td, "a");
                    a.setAttribute("href", urlQueryTreePerson(indexedDatabase, uuidLink));
                    a.setTextContent(parent.name());
                }
            }
        }

        return person;
    }

    private void fragPersonPartnerships(IndexedDatabase indexedDatabase, UUID uuidPerson, Element body) throws SQLException {
        final Optional<FullPerson> optPerson;
        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final PartnershipsMap map = session.getMapper(PartnershipsMap.class);
            optPerson = Optional.ofNullable(map.select(IndexedPerson.from(uuidPerson)));
        }

        if (optPerson.isPresent()) {
            final List<PersonPartnership> partnerships = optPerson.get().getPartnerships();
            LOG.debug("partnerships selected: {}", partnerships);
            for (final PersonPartnership partnership : partnerships) {
                UUID uuidLink= partnership.idPerson();
                final Optional<Refn> optRefn = findRefnFor(indexedDatabase, uuidLink);
                if (optRefn.isPresent()) {
                    uuidLink = optRefn.get().uuid();
                }
                final Element section = e(body, "section");
                section.setAttribute("class", "partnership");
                e(section, "hr");
                final Element table = e(section, "table");
                final Element tbody = e(table, "tbody");
                final Element tr = e(tbody, "tr");
                final Element td = e(tr, "td");

                final Element nature = e(td, "span");
                nature.setAttribute("class", "nature");
                nature.setTextContent("(" + partnership.nature() + ")");

                final Element a = e(td, "a");
                a.setAttribute("href", urlQueryTreePerson(indexedDatabase, uuidLink));
                a.setTextContent(partnership.name());

                fragPersonPartnershipChildren(indexedDatabase, partnership.id(), section);
            }
        } else {
            final Element section = e(body, "section");
            section.setAttribute("class", "partnership");
            e(section, "hr");
            final Element table = e(section, "table");
            final Element tbody = e(table, "tbody");
            final Element tr = e(tbody, "tr");
            final Element td = e(tr, "td");
            final Element span = e(td, "span");
            span.setAttribute("class", "missing");
            span.setTextContent("[no known partnerships (in this database)]");
        }
    }

    private void fragPersonPartnershipChildren(IndexedDatabase indexedDatabase, int idRelationship, Element section) throws SQLException {
        final List<PersonChild> children;
        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final ChildrenMap map = session.getMapper(ChildrenMap.class);
            children = map.select(idRelationship);
        }
        if (children.isEmpty()) {
            final Element table = e(section, "table");
            final Element tbody = e(table, "tbody");
            final Element tr = e(tbody, "tr");
            final Element td = e(tr, "td");
            final Element span = e(td, "span");
            span.setAttribute("class", "missing");
            span.setTextContent("[no known children for this partnership (in this database)]");
        } else {
            final Element table = e(section, "table");
            final Element tbody = e(table, "tbody");
            for (final PersonChild child : children) {
                UUID uuidLink= child.id();
                final Optional<Refn> optRefn = findRefnFor(indexedDatabase, uuidLink);
                if (optRefn.isPresent()) {
                    uuidLink = optRefn.get().uuid();
                }
                final Element tr = e(tbody, "tr");
                final Element td = e(tr, "td");
                final Element a = e(td, "a");
                a.setAttribute("href", urlQueryTreePerson(indexedDatabase, uuidLink));
                a.setTextContent(child.name());
            }
        }
    }

    private Connection openConnectionFor(final IndexedDatabase indexedDatabase) throws SQLException {
        final Connection conn = DriverManager.getConnection("jdbc:sqlite:"+indexedDatabase.file());
        LOG.debug("Opened JDBC Connection [{}], auto-commit={}, transaction-isolation={}", conn, conn.getAutoCommit(), conn.getTransactionIsolation());
        return conn;
    }

    private Optional<Refn> findRefnFor(final IndexedDatabase indexedDatabase, final UUID personGuid) throws SQLException {
        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            LOG.debug("Searching for REFN/UUID for Person with PersonGUID={}", personGuid);
            final RefnReverseMap map = session.getMapper(RefnReverseMap.class);
            final Optional<Refn> optRefn = Optional.ofNullable(map.select(personGuid));
            if (optRefn.isPresent()) {
                LOG.debug("For Person with PersonGUID={}, found REFN/UUID {}", personGuid, optRefn.get());
            } else {
                LOG.info("Did not find REFN for Person with PersonGUID={}", personGuid);
            }
            return optRefn;
        }
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
