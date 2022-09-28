/*
    Ftm Web View
    Web server for Family Tree Maker (decrypted) databases.
    Copyright © 2021-2022, by Christopher Alan Mosher

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package nu.mine.mosher.genealogy;

import jakarta.servlet.http.*;
import org.apache.hc.core5.net.*;
import org.apache.ibatis.session.*;
import org.apache.tika.exception.TikaException;
import org.jdom2.JDOMException;
import org.slf4j.*;
import org.sqlite.SQLiteConfig;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static nu.mine.mosher.genealogy.ContextInitializer.SQL_SESSION_FACTORY;
import static nu.mine.mosher.genealogy.HtmlUtils.*;
import static nu.mine.mosher.genealogy.StringUtils.safe;
import static nu.mine.mosher.genealogy.XmlUtils.*;

/*
TODO tasks
TODO tags
TODO submitter/copyright
TODO synch info (note: only in databases that have been sync'd)
*/

public class FtmViewerServlet extends HttpServlet {
    private static final Logger LOG =  LoggerFactory.getLogger(FtmViewerServlet.class);

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) {
        final var timestamp = Instant.now();

        LOG.trace("REQUEST HANDLING: BEGIN {}", "=".repeat(50));
        LOG.info("request timestamp: {}", timestamp);
        try {
            LOG.trace("GET {}", request.getRequestURI());
            logRequestInfo(request);
            tryGet(request, response, timestamp);
        } catch (final Throwable e) {
            LOG.error("uncaught exception in servlet", e);
        }
        LOG.trace("REQUEST HANDLING: END   {}", "=".repeat(50));
    }

    private void tryGet(final HttpServletRequest request, final HttpServletResponse response, final Instant timestamp) throws IOException, ParserConfigurationException, TransformerException, SQLException, JDOMException, SAXException, TikaException, URISyntaxException, GeneralSecurityException {
        Optional<Document> dom = Optional.empty();

        if (request.getServletPath().equals("/")) {
            dom = handleRequest(request, response, timestamp);
        } else {
            LOG.warn("Unexpected servlet path: {}", request.getServletPath());
            LOG.info("requested resource not found");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

        if (dom.isPresent()) {
            response.setContentType("application/xhtml+xml; charset=utf-8");
            final BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream());
            XmlUtils.serialize(dom.get(), out, false, true);
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
            Arrays.stream(cookies).forEach(c -> LOG.debug("cookie: {}: {}={}", Optional.ofNullable(c.getPath()).orElse("[no-path]"), c.getName(), c.getValue()));
        }
        final Map<String, String[]> parameters = request.getParameterMap();
        if (Objects.isNull(parameters) || parameters.isEmpty()) {
            LOG.debug("The request had no query parameters.");
        } else {
            parameters.entrySet().forEach(FtmViewerServlet::logQueryParams);
        }
    }

    private static void logQueryParams(final Map.Entry<String, String[]> entry) {
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

    private static String cookie(final HttpServletRequest req, final String name) {
        final Cookie[] cookies = req.getCookies();
        if (Objects.isNull(cookies) || cookies.length == 0) {
            return null;
        }
        final Optional<Cookie> optCookie = Arrays.stream(cookies).filter(c -> c.getName().equals(name)).findAny();
        if (optCookie.isEmpty()) {
            return null;
        }
        return optCookie.get().getValue();
    }

    private Optional<Document> handleRequest(final HttpServletRequest request, final HttpServletResponse response, final Instant timestamp) throws ParserConfigurationException, IOException, SQLException, JDOMException, SAXException, TikaException, TransformerException, URISyntaxException, GeneralSecurityException {
        final var now = timestamp.atZone(ZoneOffset.UTC);

        final Optional<UUID> uuidPerson = getRequestedPersonUuid(request);
        if (uuidPerson.isPresent()) {
            LOG.debug("person_uuid: {}", uuidPerson.get());
        } else {
            LOG.debug("no valid 'person_uuid' query parameter found");
        }

        final Optional<String> nameTree = getRequestedTreeName(request);
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

        final Optional<Integer> pkidCitation = getRequestedSourcePkid(request);

        final var optRbacSubject = new RbacAuthenticator(cookie(request, "idtoken")).authenticate();
        final var authorizer = new RbacAuthorizer(optRbacSubject, now);

        final var allPerms = authorizer.allPermissions();
        final var sPerms = allPerms.stream().map(Enum::toString).collect(Collectors.joining(","));
        LOG.info("Permissions granted user: {}", sPerms);




        // log request in database
        var ip = "";
        var agent = "";
        var uri = "";
        final Enumeration<String> e = request.getHeaderNames();
        while (e.hasMoreElements()) {
            final String header = e.nextElement();
            if (header.equalsIgnoreCase("X-Real-IP")) {
                ip = request.getHeader(header);
            } else if (header.equalsIgnoreCase("User-Agent")) {
                agent = request.getHeader(header);
            } else if (header.equalsIgnoreCase("Nginx-Request-URI")) {
                uri = request.getHeader(header);
            }
        }
        DatabaseHandler.logRequest(timestamp, ip, agent, uri, uuidPerson, indexedDatabase, authorizer.userID());



        Optional<Document> dom = Optional.empty();

        if (!authorizer.can(RbacPermission.PUBLIC)) {
            LOG.info("Unauthorized access blocked. Sending to home page.");
            dom = Optional.of(pageIndexDatabases(authorizer, now));
        } else if (nameTree.isPresent() && uuidPerson.isPresent()) {
            if (indexedDatabase.isPresent()) {
                final Optional<IndexedPerson> optFiltered = findPersonInTree(indexedDatabase.get(), IndexedPerson.from(uuidPerson.get()));
                if (optFiltered.isPresent()) {
                    dom = Optional.of(pagePerson(request, authorizer, indexedDatabase.get(), optFiltered.get(), now));
                } else {
                    final Optional<IndexedDatabase> indexedDatabaseOther = findPersonInAnyTree(IndexedPerson.from(uuidPerson.get()));
                    if (indexedDatabaseOther.isPresent()) {
                        redirectToTreePerson(indexedDatabaseOther.get(), IndexedPerson.from(uuidPerson.get()), response);
                    } else {
                        redirectToTree(indexedDatabase.get(), response);
                    }
                }
            } else {
                final Optional<IndexedDatabase> indexedDatabaseOther = findPersonInAnyTree(IndexedPerson.from(uuidPerson.get()));
                if (indexedDatabaseOther.isPresent()) {
                    redirectToTreePerson(indexedDatabaseOther.get(), IndexedPerson.from(uuidPerson.get()), response);
                } else {
                    LOG.info("person not currently found in any tree");
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        } else if (pkidCitation.isPresent() && indexedDatabase.isPresent()) {
            dom = Optional.of(pageSource(request, authorizer, indexedDatabase.get(), pkidCitation.get(), now));
        } else if (nameTree.isPresent()) {
            if (indexedDatabase.isPresent()) {
                dom = Optional.of(pageIndexPeople(authorizer, indexedDatabase.get(), now));
            } else {
                LOG.info("tree does not currently exist");
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } else if (uuidPerson.isPresent()) {
            final Optional<IndexedDatabase> indexedDatabaseOther = findPersonInAnyTree(IndexedPerson.from(uuidPerson.get()));
            if (indexedDatabaseOther.isPresent()) {
                redirectToTreePerson(indexedDatabaseOther.get(), IndexedPerson.from(uuidPerson.get()), response);
            } else {
                LOG.info("person not currently found in any tree");
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } else {
            dom = Optional.of(pageIndexDatabases(authorizer, now));
        }

        return dom;
    }

    private static void redirectToTree(final IndexedDatabase indexedDatabase, final HttpServletResponse response) throws URISyntaxException {
        final String location = urlQueryTree(indexedDatabase);
        response.setHeader("Location", location);
        LOG.info("redirecting to: {}", location);
        response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
    }


    private static void redirectToTreePerson(final IndexedDatabase indexedDatabase, final IndexedPerson indexedPerson, final HttpServletResponse response) throws URISyntaxException {
        final String location = urlQueryTreePerson(indexedDatabase, indexedPerson);
        response.setHeader("Location", location);
        LOG.info("redirecting to: {}", location);
        response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
    }

    private static String urlQueryTreePerson(final IndexedDatabase indexedDatabase, final IndexedPerson indexedPerson) throws URISyntaxException {
        return new URIBuilder().
            addParameter("tree",indexedDatabase.file().getName()).
            addParameter("person_uuid", indexedPerson.preferRefn().toString()).
            build().
            toString();
    }

    private static String urlQueryTree(final IndexedDatabase indexedDatabase) throws URISyntaxException {
        return new URIBuilder().
            addParameter("tree",indexedDatabase.file().getName()).
            build().
            toString();
    }

    public static String urlQueryTreeSource(final IndexedDatabase indexedDatabase, final int pkidCitation) throws URISyntaxException {
        return new URIBuilder().
            addParameter("tree",indexedDatabase.file().getName()).
            addParameter("source", ""+pkidCitation).
            build().
            toString();
    }

    /*
    TODO change the find/find-any logic to work more like:
    1. given a uuid and optionally a preferred tree,
    2. if pref. tree given, then search for uuid in preferred tree, if found return
    3. otherwise search all other trees
    */

    private Optional<IndexedDatabase> findPersonInAnyTree(final IndexedPerson indexedPerson) throws SQLException {
        final List<IndexedDatabase> dbs = loadDatabaseIndex();
        for (final IndexedDatabase db : dbs) {
            final Optional<IndexedPerson> optPerson = findPersonInTree(db, indexedPerson);
            if (optPerson.isPresent()) {
                return Optional.of(db);
            }
        }
        return Optional.empty();
    }

    private Optional<IndexedPerson> findPersonInTree(final IndexedDatabase indexedDatabase, final IndexedPerson indexedPerson) throws SQLException {
        final Optional<IndexedPerson> optFiltered;

        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final PersonQuickMap map = session.getMapper(PersonQuickMap.class);
            final var idPerson = map.select(indexedPerson.preferRefn().toString());
            if (Objects.nonNull(idPerson) && idPerson > 0L) {
                LOG.debug("Found matching Person.ID: {}", idPerson);
                optFiltered = Optional.ofNullable(session.getMapper(PersonMap.class).select(idPerson));
                if (optFiltered.isEmpty()) {
                    LOG.warn("Could not re-locate Person.ID: {}", idPerson);
                }
            } else {
                LOG.info("Did not find any Person associated with UUID: {}", indexedPerson.preferRefn());
                optFiltered = Optional.empty();
            }
        }

        return optFiltered;
    }

    private static Optional<IndexedDatabase> findTree(final String nameTree) {
        for (final IndexedDatabase idb : loadDatabaseIndex()) {
            if (idb.file().getName().equals(nameTree)) {
                return Optional.of(idb);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> getRequestedTreeName(final HttpServletRequest request) {
        final Optional<String> optNameTree = Optional.ofNullable(request.getParameter("tree"));
        if (optNameTree.isPresent() && !optNameTree.get().isBlank()) {
            return optNameTree;
        }
        return Optional.empty();
    }

    private static Optional<UUID> getRequestedPersonUuid(final HttpServletRequest request) {
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

    private static Optional<Integer> getRequestedSourcePkid(final HttpServletRequest request) {
        final Optional<String> optPkidSource = Optional.ofNullable(request.getParameter("source"));
        if (optPkidSource.isPresent() && !optPkidSource.get().isBlank()) {
            try {
                return Optional.of(Integer.parseInt(optPkidSource.get()));
            } catch (final Throwable e) {
                LOG.info("Invalid format for source ID query parameter: {}", optPkidSource.get());
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Document pageIndexDatabases(final RbacAuthorizer role, ZonedDateTime now) throws ParserConfigurationException, SQLException, URISyntaxException {
        final Document dom = XmlUtils.empty();



        final Element html = e(dom, "html");
        html.setAttribute("class", "fontFeatures unicodeWebFonts solarizedLight");



        final Element head = e(html, "head");

        final Element title = e(head, "title");
        title.setTextContent("Databases");

        final Element css = e(head, "link");
        css.setAttribute("rel", "stylesheet");
        css.setAttribute("href", "./assets/styles/nu/mine/mosher/genealogy/page-dbs.css");

        addAuthHead(head);

        final Element body = e(html, "body");

        fragNav(role, null, null, body);

        e(body, "hr");

        final Element header = e(body, "header");
        final Element h1 = e(header, "h1");
        h1.setTextContent("Browse a genealogy database");

        e(body, "hr");

        final Element section = e(body, "section");
        final Element ul = e(section, "ul");
        final List<IndexedDatabase> dbs = loadDatabaseIndex();
        dbs.sort((o1, o2) -> o1.file().getName().toLowerCase().compareToIgnoreCase(o2.file().getName().toLowerCase()));
        for (final IndexedDatabase iDb : dbs) {
            final Element li = e(ul, "li");
            final Element link = e(li, "a");
            link.setAttribute("href", urlQueryTree(iDb));
            link.setTextContent("{"+iDb.file().getName()+"}");
        }

        e(body, "hr");

        fragFooter(Optional.empty(), body, now);

        return dom;
    }

    private Document pageIndexPeople(RbacAuthorizer role, final IndexedDatabase indexedDatabase, ZonedDateTime now) throws ParserConfigurationException, SQLException, URISyntaxException {
        final List<IndexedPerson> list;
        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final PersonIndexMap map = session.getMapper(PersonIndexMap.class);
            list = map.select();
            list.sort(Comparator.naturalOrder());
        }



        final Document dom = XmlUtils.empty();






        final Element html = e(dom, "html");
        html.setAttribute("class", "fontFeatures unicodeWebFonts solarizedLight");



        final Element head = e(html, "head");
        final Element title = e(head, "title");
        title.setTextContent(indexedDatabase.file().getName());

        final Element css = e(head, "link");
        css.setAttribute("rel", "stylesheet");
        css.setAttribute("href", "./assets/styles/nu/mine/mosher/genealogy/page-people.css");

        addAuthHead(head);


        final Element body = e(html, "body");

        fragNav(role, indexedDatabase, null, body);

        e(body, "hr");

        final Element header = e(body, "header");
        final Element h1 = e(header, "h1");
        h1.setTextContent(indexedDatabase.file().getName());

        e(body, "hr");

        final Element section = e(body, "section");

        final Element ul = e(section, "ul");
        Styles.add(ul, Styles.Layout.cn);
        for (final IndexedPerson indexedPerson : list) {
            boolean can = false;
            if (role.can(RbacPermission.LIST)) {
                can = role.can(indexedPerson.isRecent() ? RbacPermission.PRIVATE : RbacPermission.PUBLIC);
            }
            if (can) {
                final Element li = e(ul, "li");
                Styles.add(li, Styles.Render.hanging);

                final Element spDates = e(li, "span");
                Styles.add(spDates, Styles.Render.smaller);
                Styles.add(spDates, Styles.Render.dim);
                spDates.setTextContent(indexedPerson.dates() + " ");

                final Element ap = e(li, "a");
                ap.setAttribute("href", urlQueryTreePerson(indexedDatabase, indexedPerson));
                ap.setTextContent(indexedPerson.name());
            }
        }

        e(body, "hr");

        fragFooter(Optional.empty(), body, now);

        return dom;
    }

    private void addAuthHead(final Element head) {
        Element e;

        e = e(head, "script");
        e.setAttribute("src", "https://cdn.jsdelivr.net/npm/js-cookie@3.0.1/dist/js.cookie.min.js");

        e = e(head, "script");
        e.setAttribute("src", "https://accounts.google.com/gsi/client");
        e.setAttribute("async", "async");

        e = e(head, "script");
        e.setAttribute("src", "./assets/scripts/nu/mine/mosher/google.js");
        e.setAttribute("async", "async");
    }

    private void addNoMenuHead(final Element head) {
        Element e;
        e = e(head, "script");
        e.setAttribute("src", "./assets/scripts/nu/mine/mosher/nomenu.js");
        e.setAttribute("async", "async");
    }

    private void addCopyCitationHead(final Element head) {
        Element e;
        e = e(head, "script");
        e.setAttribute("src", "./assets/scripts/nu/mine/mosher/citation.js");
        e.setAttribute("async", "async");
    }

    private void addAuthNav(final RbacAuthorizer role, final Element nav) {
        Element e;

        if (!role.authenticated()) {
            // sign-in button
            e = e(nav, "div");
            e.setAttribute("id", "g_id_onload");
            e.setAttribute("data-client_id", System.getenv("CLIENT_ID"));
            e.setAttribute("data-callback", "onSignIn");
            e.setAttribute("data-context", "signin");
            e.setAttribute("data-ux_mode", "popup");
            e.setAttribute("data-auto_prompt", "true");
            e.setAttribute("data-auto_select", "true");
            e.setAttribute("data-itp_support", "true");

            e = e(nav, "div");
            e.setAttribute("class", "g_id_signin layout-right");
            e.setAttribute("data-type", "icon");
            e.setAttribute("data-shape", "circle");
            e.setAttribute("data-theme", "outline");
            e.setAttribute("data-text", "signin_with");
            e.setAttribute("data-size", "large");
        } else {
            // signed-in user's email
            e = e(nav, "small");
            e.setTextContent(role.display());
            Styles.add(e, Styles.Render.hiauth);

            e = e(nav, "span");
            e.setTextContent(" ");

            // sign-out button
            e = e(nav, "a");
            Styles.add(e, Styles.Links.button);
            e.setTextContent("Sign\u00A0out");
            e.setAttribute("id", "signout");
        }
    }

    private void fragNav(RbacAuthorizer role, final IndexedDatabase indexedDatabase, IndexedPerson indexedPerson, final Element parent) throws SQLException, URISyntaxException {
        final Element header = e(parent, "header");
        final Element nav = e(header, "nav");
        Styles.add(nav, Styles.Layout.c2Wrap);

        final Element divL = e(nav, "div");
        Styles.add(divL, Styles.Layout.c2Left);

        Element sp;
        final Element a = e(divL, "a");
        a.setAttribute("href", "./");
        a.setTextContent("{home}");

        if (Objects.nonNull(indexedDatabase)) {
            sp = e(divL, "span");
            sp.setTextContent(" ");
            final Element a2 = e(divL, "a");
            a2.setAttribute("href", urlQueryTree(indexedDatabase));
            a2.setTextContent("{" + indexedDatabase.file().getName() + "}");
        }

        LOG.debug("source database: {}", indexedDatabase);
        if (Objects.nonNull(indexedPerson)) {
            final List<IndexedDatabase> dbs = loadDatabaseIndex();
            boolean labeled = false;
            for (final IndexedDatabase db : dbs) {
                if (db.file().getAbsolutePath().equals(indexedDatabase.file().getAbsolutePath())) {
                    LOG.debug("skipping check in source database: {}",db);
                } else {
                    LOG.debug("checking database: {}",db);
                    final Optional<IndexedPerson> optPerson = findPersonInTree(db, indexedPerson);
                    if (optPerson.isPresent()) {
                        LOG.debug("Found person {} in alternate tree {}", optPerson.get(), db);
                        if (!labeled) {
                            final Element span = e(divL, "span");
                            span.setTextContent(" see also:");
                            labeled = true;
                        }
                        sp = e(divL, "span");
                        sp.setTextContent(" ");
                        final Element a3 = e(divL, "a");
                        a3.setAttribute("href", urlQueryTreePerson(db, optPerson.get()));
                        a3.setTextContent("{" + db.file().getName() + "}");
                    }
                }
            }

        }



        final Element divR = e(nav, "div");
        Styles.add(divR, Styles.Layout.c2Right);

        addAuthNav(role, divR);
    }

    private SqlSession openSessionFor(final Connection connection) {
        final SqlSessionFactory sqlSessionFactory = (SqlSessionFactory)getServletContext().getAttribute(SQL_SESSION_FACTORY);
        return sqlSessionFactory.openSession(connection);
    }

    private Document pagePerson(final HttpServletRequest req, RbacAuthorizer role, final IndexedDatabase indexedDatabase, final IndexedPerson indexedPerson, ZonedDateTime now) throws ParserConfigurationException, SQLException, JDOMException, SAXException, TikaException, IOException, TransformerException, URISyntaxException {
        final Person person = loadPersonDetails(indexedDatabase, indexedPerson);
        final Footnotes<EventSource> footnotes = new Footnotes<>();



        final Document dom = XmlUtils.empty();

        final Element html = e(dom, "html");
        html.setAttribute("class", "fontFeatures unicodeWebFonts solarizedLight");



        final Element head = e(html, "head");

        final Element title = e(head, "title");
        title.setTextContent(styleName(person.nameWithSlashes()));

        final Element css = e(head, "link");
        css.setAttribute("rel", "stylesheet");
        css.setAttribute("href", "./assets/styles/nu/mine/mosher/genealogy/page-person.css");

        addAuthHead(head);
        addNoMenuHead(head);
        addCopyCitationHead(head);

        final Element body = e(html, "body");

        fragNav(role, indexedDatabase, indexedPerson, body);
        if (role.can(RbacPermission.READ)) {
            e(body, "hr");
            fragPersonParents(indexedDatabase, indexedPerson, body);
        }
        e(body, "hr");
        fragName(indexedDatabase, indexedPerson, person, body,  footnotes);
        e(body, "hr");
        if (role.can(RbacPermission.READ)) {
            fragEvents(role, indexedDatabase, new FtmLink(FtmLinkTableID.Person, person.pkid()), body, footnotes);
            fragPersonPartnerships(role, indexedDatabase, indexedPerson, body, footnotes);
        } else {
            fragNotice(body);
        }
        e(body, "hr");
        fragCite(req, body, person, indexedDatabase, now);
        if (role.can(RbacPermission.READ)) {
            e(body, "hr");
            fragFootnotes(req, indexedDatabase, body, footnotes);
        }
        e(body, "hr");
        fragFooter(Optional.of(person), body, now);



        return dom;
    }

    private void fragCite(final HttpServletRequest req, final Element body, final Person person, final IndexedDatabase indexedDatabase, final ZonedDateTime now) {
        final var div = e(body, "div");



        final var button = e(div, "button");
        button.setAttribute("class", "tei-button-copy");
        button.setAttribute("data-html-for", "urn:uuid:032a6ed6-40da-4687-85ab-44b25739e275");
        t(button, "Copy source citation");
        t(div, " ");



        final var citation = e(div, "div");
        citation.setAttribute("id", "urn:uuid:032a6ed6-40da-4687-85ab-44b25739e275");
        citation.setAttribute("class", Styles.Render.smaller +" tei-inline");

        final var optAuthor = Optional.ofNullable(System.getenv("FTM_AUTHOR"));
        var ctext = new StringBuilder(256);
        if (optAuthor.isPresent()) {
            ctext.append(optAuthor.get()).append(", ");
        }
        ctext.append("\u201C");
        ctext.append(styleName(person.nameWithSlashes()));
        ctext.append("\u201D, ");
        t(citation, ctext.toString());

        final var titl = e(citation, "i");
        t(titl, indexedDatabase.file().getName());

        ctext = new StringBuilder(256);
        ctext.append(", database (");
        ctext.append(urlOfThisPage(req));
        ctext.append(" : accessed ");
        final String sNow = now.format(DateTimeFormatter.ISO_LOCAL_DATE);
        ctext.append(sNow);
        ctext.append(").");
        t(citation, ctext.toString());
    }

    private String urlOfThisPage(final HttpServletRequest req) {
        var scheme = req.getHeader("x-forwarded-proto");
        if (scheme == null || scheme.isBlank()) {
            scheme = req.getScheme();
        }

        var host = req.getHeader("x-forwarded-host");
        if (host == null || host.isBlank()) {
            host = req.getServerName();
        }

        var port = req.getHeader("x-forwarded-port");
        if (port == null || port.isBlank()) {
            port = "";
        }

        var pre = req.getHeader("x-forwarded-prefix");
        if (pre == null || pre.isBlank()) {
            pre = "";
        } else {
            if (pre.startsWith("/")) {
                pre = pre.substring(1);
            }
            if (!pre.endsWith("/")) {
                pre = pre + "/";
            }
        }

        final var nport = getPortOrDefault(port, scheme);

        return scheme +
            "://" +
            host +
            (nport == 0 ? "" : ":" + nport) +
            "/" +
            pre +
            "?" +
            req.getQueryString();
    }

    private static int getPortOrDefault(final String port, final String scheme) {
        int p;
        try {
            p = Integer.parseInt(port);
        } catch (final Exception ignore) {
            p = 0;
        }
        if (scheme.equals("https") && p == 443) {
            return 0;
        }
        if (scheme.equals("http") && p == 80) {
            return 0;
        }
        return p;
    }

    private void fragFootnotes(final HttpServletRequest req, final IndexedDatabase indexedDatabase, final Element body, final Footnotes<EventSource> footnotes) throws JDOMException, SAXException, TikaException, IOException, TransformerException, ParserConfigurationException, URISyntaxException {
        final int n = footnotes.size();
        if (n <= 0) {
            return;
        }

        final Element section = e(body, "section");
        final Element ul = e(section, "ul");

        for (int i = 1; i <= n; ++i) {
            final Element li = e(ul, "li");

            final Element footnote = e(li, "p");
            footnote.setAttributeNS(XHTML_NAMESPACE, "id", String.format("f%d", i));
            Styles.add(footnote, Styles.Render.smaller);
            Styles.add(footnote, Styles.Render.indent);
            Styles.add(footnote, Styles.Render.eqlines);

            final Element sup = e(footnote, "sup");
            final Element footnum = e(sup, "span");
            footnum.setTextContent(formatFootnum(i, n));

            footnotes.getFootnote(i).appendTo(req, footnote, indexedDatabase);
        }
    }

    private String formatFootnum(int i, int n) {
        final StringBuilder s = new StringBuilder(2);
        if (i < 10 && 10 <= n) { // TODO refactor
            s.append("\u2007");
        }
        s.append(i);
        return s.toString();
    }

    private void fragEvents(final RbacAuthorizer role, final IndexedDatabase indexedDatabase, final FtmLink link, final Element body, Footnotes<EventSource> footnotes) throws SQLException {
        final List<Event> events;
        final Map<Integer,EventWithSources> mapEventSources;
        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final EventsMap map = session.getMapper(EventsMap.class);
            events = map.select(link);
            mapEventSources =
                session.
                getMapper(EventSourcesMap.class).
                select(link).
                stream().
                collect(Collectors.toMap(e -> e.pkidFact, e -> e));
        }

        LOG.debug("Events: {}", events);
        LOG.debug("EventWithSources: {}", mapEventSources);

        abbreviatePlacesOf(events);

        final Element section = e(body, "section");
        final Element table = e(section, "table");
        Styles.add(table, Styles.Layout.indent);
        final Element tbody = e(table, "tbody");
        for (final Event event : events) {
            fragEvent(role, footnotes, mapEventSources, tbody, event);
        }
    }

    private void fragEvent(final RbacAuthorizer role, final Footnotes<EventSource> footnotes, final Map<Integer, EventWithSources> mapEventSources, final Element tbody, final Event event) throws SQLException {
        if (!event.isRecent() || role.can(RbacPermission.PRIVATE)) {
            final Element tr = e(tbody, "tr");

            final Element tdDate = e(tr, "td");
            Styles.add(tdDate, Styles.Render.nowrap);
            final Element spanDate = e(tdDate, "span");
            ifPresent(event.date(), spanDate);
            Styles.add(spanDate, getEventHighlight(event));

            final Element tdPlace = e(tr, "td");
            final Place place = event.place();
            if (place.isBlank()) {
                ifPresent(null, tdPlace);
            } else {
                place.appendTo(tdPlace);
            }

            final Element tdDescription = e(tr, "td");
            final Element spanType = e(tdDescription, "span");
            ifPresent(event.type(), spanType);
            Styles.add(spanType, getEventHighlight(event));
            if (Objects.nonNull(event.description()) && !event.description().isBlank()) {
                final Element spanSep = e(tdDescription, "span");
                spanSep.setTextContent(": ");
                final Element spanDesc = e(tdDescription, "span");
                final Optional<WorldTreeID> optionalWorldTreeID = event.getIfID();
                if (optionalWorldTreeID.isPresent() && optionalWorldTreeID.get().urlFor(event.description()).isPresent()) {
                    final Element a = e(spanDesc, "a");
                    a.setAttribute("href", optionalWorldTreeID.get().urlFor(event.description()).get().toExternalForm());
                    a.setTextContent(event.description());
                } else {
                    spanDesc.setTextContent(event.description());
                }
            }

            final Optional<EventWithSources> sources = Optional.ofNullable(mapEventSources.get(event.pkid()));
            if (sources.isPresent() && !sources.get().getSources().isEmpty()) {
                sources.get().sources.forEach(s -> {
                    final int footnum = footnotes.putFootnote(s);
                    final Element sup = e(tdDescription, "sup");
                    final Element footref = e(sup, "a");
                    footref.setAttribute("href", "#f" + footnum);
                    footref.setTextContent("[" + footnum + "]");
                });
            }
        }
    }

    private Styles.Render getEventHighlight(final Event event) {
        return
            event.isPrimary() ?
                Styles.Render.hi1 :
            event.isSecondary() ?
                Styles.Render.hi2 :
                Styles.Render.hi3;
    }

    private static void abbreviatePlacesOf(final List<Event> events) {
        final List<List<String>> places = events.stream().map(Event::place).map(Place::getHierarchy).toList();
        LOG.debug("extracted places: {}", places);
        final List<List<String>> abbrevs = new PlaceListAbbrev().abbrev(places);
        if (abbrevs.size() != places.size()) {
            LOG.error("Unexpected size of place list after abbreviation. Ignoring abbreviations.");
            return;
        }

        Place prev = null;
        for (int i = 0; i < abbrevs.size(); ++i) {
            final Place place = events.get(i).place();
            if (place.equals(prev) && !place.isBlank()) {
                place.setDitto();
            } else {
                place.setAbbreviatedOverride(abbrevs.get(i));
                prev = place;
            }
        }
    }

    private static void ifPresent(Object it, Element e) {
        e.setTextContent(
            Optional.ofNullable(it).
            map(Object::toString).
            filter(s -> !s.isBlank()).
            orElse("\u00a0\u2e3a"));
    }

    private void fragName(final IndexedDatabase indexedDatabase, final IndexedPerson indexedPerson, final Person person, final Element body, Footnotes<EventSource> footnotes) throws SQLException, URISyntaxException {
        final Element header = e(body, "header");
        final Element h1 = e(header, "h1");
        final Element sup = e(h1, "sup");
        final Element a = e(sup, "a");
        a.setTextContent(new String(Character.toChars(0x1F517)));
        a.setAttribute("title", "permanent link to this person");
        a.setAttribute("href", urlQueryTreePerson(indexedDatabase, indexedPerson));

        final Element personName = e(h1, "span");
        personName.setTextContent(styleName(person.nameWithSlashes()));
        Styles.add(personName, Styles.Render.hi0);

        final FtmLink linkPerson = new FtmLink(FtmLinkTableID.Person, person.pkid());
        fragNoteRefs(indexedDatabase, linkPerson, h1, footnotes);
    }

    private static String styleName(String n) {
        n = n
            .replaceAll(" /"," ")
            .replaceAll("/ ", " ")
            .replaceAll("^/", "")
            .replaceAll("/$", "");
        return n;
    }

    private void fragNoteRefs(final IndexedDatabase indexedDatabase, final FtmLink link, final Element parent, final Footnotes<EventSource> footnotes) throws SQLException {
        final List<Note> notes;
        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final NotesMap map = session.getMapper(NotesMap.class);
            notes = map.select(link);
        }

        notes.forEach(s -> {
            final String note = safe(s.note());
            if (!note.isBlank()) {
                final int footnum = footnotes.putFootnote(EventSource.fromNote(note));
                final Element sup = e(parent, "sup");
                final Element footref = e(sup, "a");
                footref.setAttribute("href", "#f" + footnum);
                footref.setTextContent("[" + footnum + "]");
            }
        });
    }

    private static void fragNotice(final Element body) {
        final Element p = e(body, "p");
        p.setTextContent("[You must sign in using your Google identity to view the details of this person.]");
    }

    private static void fragFooter(final Optional<Person> person, final Element body, final ZonedDateTime now) {
        final Element footer = e(body, "footer");

        final Element ul = e(footer, "ul");

        if (person.isPresent() && Objects.nonNull(person.get().lastmod())) {
            final Element li = e(ul, "li");
            final Element small = e(li, "small");
            final Element tsLastMod = e(small, "span");
            tsLastMod.setTextContent(person.get().lastmod() + " : person last modified");
        }

        {
            final Element li = e(ul, "li");
            final Element small = e(li, "small");
            final Element tsPage = e(small, "span");
            final String sNow = now.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
            tsPage.setTextContent(sNow + " : page generated");
        }

        {
            final Element li = e(ul, "li");
            final Element small = e(li, "small");
            final Element copyright = e(small, "span");
            copyright.setTextContent(Objects.requireNonNullElse(System.getenv("FTM_COPYRIGHT"), "Copyright © by the owners. All rights reserved."));
        }
    }

    private Person loadPersonDetails(final IndexedDatabase indexedDatabase, final IndexedPerson indexedPerson) throws SQLException {
        final Person person;
        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final PersonDetailsMap map = session.getMapper(PersonDetailsMap.class);
            person = map.select(indexedPerson);
        }
        LOG.debug("Loaded Person: {}", person);
        return person;
    }

    private void fragPersonParents(final IndexedDatabase indexedDatabase, final IndexedPerson indexedPerson, final Element body) throws SQLException, URISyntaxException {
        final List<PersonParent> parents;
        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final ParentsMap map = session.getMapper(ParentsMap.class);
            parents = filterParents(map.select(indexedPerson));
        }

        final Element section = e(body, "section");
        final Element table = e(section, "table");
        final Element tbody = e(table, "tbody");
        if (parents.isEmpty()) {
            final Element tr = e(tbody, "tr");
            final Element td = e(tr, "td");
            final Element span = e(td, "span");
            span.setTextContent("[no known parents (in this database)]");
        } else {
            for (final PersonParent parent : parents) {
                UUID uuidLink= parent.id();
                final Optional<Refn> optRefn = findRefnFor(indexedDatabase, uuidLink);
                if (optRefn.isPresent()) {
                    uuidLink = optRefn.get().uuid();
                }
                final Element tr = e(tbody, "tr");
                final Element td = e(tr, "td");
                if (parent.nature().display()) {
                    final Element nature = e(td, "span");
                    nature.setTextContent("(" + parent.nature() + ") ");
                }
                final Element a = e(td, "a");
                Styles.add(a, Styles.Links.hilite);
                a.setAttribute("href", urlQueryTreePerson(indexedDatabase, IndexedPerson.from(uuidLink)));
                a.setTextContent(parent.name());

                if (0 < parent.grandparents()) {
                    final Element gc = e(td, "sup");
                    gc.setTextContent("\u2442".repeat(parent.grandparents()));
                }
            }
        }
    }

    private List<PersonParent> filterParents(final List<PersonParent> parents) {
        final List<PersonParent> r = new ArrayList<>();
        if (Objects.isNull(parents)) {
            LOG.warn("parents array reference is null.");
        } else if (parents.isEmpty()) {
            LOG.warn("parents array reference is empty.");
        } else {
            for (final PersonParent parent : parents) {
                LOG.warn("parent {}", parent);
                if (Objects.isNull(parent)) {
                    LOG.warn("For parents, ignoring NULL entry.");
                } else if (Objects.isNull(parent.id())) {
                    LOG.warn("For parents, ignoring person with NULL id: {}", parent);
                } else {
                    LOG.debug("will use parent: {}", parent);
                    r.add(parent);
                }
            }
        }
        return r;
    }

    private void fragPersonPartnerships(final RbacAuthorizer role, final IndexedDatabase indexedDatabase, final IndexedPerson indexedPerson, final Element body, Footnotes<EventSource> footnotes) throws SQLException, URISyntaxException {
        final List<PersonPartnership> partnerships;

        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final PartnershipsMap map = session.getMapper(PartnershipsMap.class);
            partnerships = map.select(indexedPerson);
        }

        if (Objects.isNull(partnerships) || partnerships.isEmpty()) {
            LOG.debug("no partnerships selected");
            e(body, "hr");
            final Element section = e(body, "section");
            final Element span = e(section, "span");
            span.setTextContent("[no known partnerships (in this database)]");
        } else {
            LOG.debug("partnerships selected: {}", partnerships);
            for (final PersonPartnership partnership : partnerships) {
                if (!partnership.isRecent() || role.can(RbacPermission.PRIVATE)) {
                    UUID uuidLink = partnership.idPerson();
                    final Optional<Refn> optRefn = findRefnFor(indexedDatabase, uuidLink);
                    if (optRefn.isPresent()) {
                        uuidLink = optRefn.get().uuid();
                    }
                    e(body, "hr");
                    final Element section = e(body, "section");

                    if (partnership.nature().display()) {
                        final Element nature = e(section, "span");
                        nature.setTextContent("(" + partnership.nature() + ") ");
                    }

                    if (partnership.status().display()) {
                        final Element nature = e(section, "span");
                        nature.setTextContent("(" + partnership.status() + ") ");
                    }

                    if (Objects.isNull(uuidLink)) {
                        final Element span = e(section, "span");
                        span.setTextContent("[unknown partner]");
                    } else {
                        final Element span = e(section, "span");
                        span.setTextContent("partner: ");
                        final Element a = e(section, "a");
                        Styles.add(a, Styles.Links.hilite);
                        a.setAttribute("href", urlQueryTreePerson(indexedDatabase, IndexedPerson.from(uuidLink)));
                        a.setTextContent(partnership.name());
                    }

                    final FtmLink linkRelationship = new FtmLink(FtmLinkTableID.Relationship, partnership.id());

                    fragNoteRefs(indexedDatabase, linkRelationship, section, footnotes);

                    fragEvents(role, indexedDatabase, linkRelationship, section, footnotes);

                    fragPersonPartnershipChildren(indexedDatabase, indexedPerson, partnership.id(), section);

                    // TODO: how would it look if children's births were merged with partnership events?
                }
            }
        }
    }

    private void fragPersonPartnershipChildren(final IndexedDatabase indexedDatabase, final IndexedPerson indexedPerson, final int idRelationship, final Element section) throws SQLException, URISyntaxException {
        final List<PersonChild> children;
        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final ChildrenMap map = session.getMapper(ChildrenMap.class);
            children = map.select(new ChildrenMap.ParentRel(idRelationship, indexedPerson.pkid()));
        }
        LOG.debug("Loaded children: {}", children);

        final Element ch = e(section, "span");
        ch.setTextContent("children: ");
        final Element table = e(section, "table");
        Styles.add(table, Styles.Layout.indent);
        final Element tbody = e(table, "tbody");
        if (Objects.isNull(children) || children.isEmpty()) {
            final Element tr = e(tbody, "tr");
            final Element td = e(tr, "td");
            final Element span = e(td, "span");
            span.setTextContent("[no known children for this partnership (in this database)]");
        } else {
            for (final PersonChild child : children) {
                UUID uuidLink= child.id();
                final Optional<Refn> optRefn = findRefnFor(indexedDatabase, uuidLink);
                if (optRefn.isPresent()) {
                    uuidLink = optRefn.get().uuid();
                }
                final Element tr = e(tbody, "tr");

                final Element tdDate = e(tr, "td");
                Styles.add(tdDate, Styles.Render.nowrap);
                final Element spanDate = e(tdDate, "span");
                ifPresent(child.dateBirth(), spanDate);
                Styles.add(spanDate, Styles.Render.hi1);

                final Element td = e(tr, "td");
                if (child.nature().display()) {
                    final Element span = e(td, "span");
                    span.setTextContent("("+child.nature()+") ");
                }
                final Element a = e(td, "a");
                Styles.add(a, Styles.Links.hilite);
                a.setAttribute("href", urlQueryTreePerson(indexedDatabase, IndexedPerson.from(uuidLink)));
                a.setTextContent(child.name());

                if (0 < child.grandchildren()) {
                    final Element gc = e(td, "sup");
                    gc.setTextContent("\u2443".repeat((child.grandchildren() + 2) / 3));
                }
            }
        }
    }

    private Document pageSource(final HttpServletRequest req, final RbacAuthorizer role, final IndexedDatabase indexedDatabase, final int pkidCitation, ZonedDateTime now) throws ParserConfigurationException, SQLException, JDOMException, SAXException, TikaException, TransformerException, IOException, URISyntaxException {
        final EventSource eventSource;
        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final SourceMap map = session.getMapper(SourceMap.class);
            eventSource = map.select(pkidCitation);
        }

        final Document dom = XmlUtils.empty();

        final Element html = e(dom, "html");
        html.setAttribute("class", "fontFeatures unicodeWebFonts solarizedLight");



        final Element head = e(html, "head");

        final Element title = e(head, "title");
        title.setTextContent(eventSource.title());

        final Element css2 = e(head, "link");
        css2.setAttribute("rel", "stylesheet");
        css2.setAttribute("href", "./assets/styles/nu/mine/mosher/genealogy/page-source.css");

        addAuthHead(head);
        addNoMenuHead(head);

        final Element body = e(html, "body");

        fragNav(role, indexedDatabase, null, body);

        e(body, "hr");

        final Element sectionCitation = e(body, "section");
        final Element divCitation = e(sectionCitation, "div");
        eventSource.appendTo(req, divCitation, indexedDatabase);

        e(body, "hr");

        final Element sectionTranscript = e(body, "section");
        final Element divTranscript = e(sectionTranscript, "div");

        if (safe(eventSource.comment()).isBlank()) {
            divTranscript.setTextContent("[A transcription of this page is not available.]");
        } else {
            final String trans = eventSource.comment();
            final Node node;
            if (!teiStyleIfPossible(req, trans, divTranscript)) {
                if (looksLikeHtml(trans)) {
                    node = html(trans);
                } else {
                    node = HtmlUtils.tika(trans);
                }
                divTranscript.appendChild(divTranscript.getOwnerDocument().importNode(node, true));
            }
        }

        e(body, "hr");

        fragFooter(Optional.empty(), body, now);

        return dom;
    }

    private static Connection openConnectionFor(final IndexedDatabase indexedDatabase) throws SQLException {
        final SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);
        final Connection conn = config.createConnection("jdbc:sqlite:"+indexedDatabase.file());
        LOG.debug("Opened JDBC Connection [{}], db={}, auto-commit={}, transaction-isolation={}", conn, indexedDatabase.file(), conn.getAutoCommit(), conn.getTransactionIsolation());
        return conn;
    }

    private Optional<Refn> findRefnFor(final IndexedDatabase indexedDatabase, final UUID personGuid) throws SQLException {
        if (Objects.isNull(personGuid)) {
            LOG.info("Skipping REFN search for PersonGUID=NULL");
            return Optional.empty();
        }
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
        return
            Arrays.stream(dirDbs.toFile().listFiles(ftmDbFilter())).
            map(IndexedDatabase::new).
            collect(Collectors.toList());
    }

    private static FileFilter ftmDbFilter() {
        return f -> f.isFile() && f.canRead() && f.getName().toLowerCase().endsWith(".ftm");
    }
}
