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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static nu.mine.mosher.gedcom.ContextInitializer.SQL_SESSION_FACTORY;
import static nu.mine.mosher.gedcom.XmlUtils.XHTML_NAMESPACE;
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
            Arrays.stream(cookies).forEach(c -> LOG.debug("cookie: {}: {}={}", c.getPath(), c.getName(), c.getValue()));
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
                final Optional<IndexedPerson> optFiltered = findPersonInTree(indexedDatabase.get(), IndexedPerson.from(uuidPerson.get()));
                if (optFiltered.isPresent()) {
                    dom = Optional.of(pagePerson(indexedDatabase.get(), optFiltered.get()));
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
        } else if (nameTree.isPresent()) {
            if (indexedDatabase.isPresent()) {
                dom = Optional.of(pageIndexPeople(indexedDatabase.get()));
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


    private static void redirectToTreePerson(final IndexedDatabase indexedDatabase, final IndexedPerson indexedPerson, final HttpServletResponse response) {
        final String location = urlQueryTreePerson(indexedDatabase, indexedPerson);
        response.setHeader("Location", location);
        LOG.info("redirecting to: {}", location);
        response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
    }

    private static String urlQueryTreePerson(final IndexedDatabase indexedDatabase, final IndexedPerson indexedPerson) {
        return "?"+URLEncodedUtils.format(
            List.of(
                new BasicNameValuePair("tree", indexedDatabase.file().getName()),
                new BasicNameValuePair("person_uuid", indexedPerson.preferRefn().toString())),
            StandardCharsets.UTF_8);
    }

    private static String urlQueryTree(final IndexedDatabase indexedDatabase) {
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
            final PersonMap map = session.getMapper(PersonMap.class);
            optFiltered = Optional.ofNullable(map.select(indexedPerson));
            if (optFiltered.isPresent()) {
                LOG.debug("Found matching Person: {}", optFiltered.get());
            } else {
                LOG.info("Did not find Person matching {}", indexedPerson);
            }
        }

        return optFiltered;
    }

//    private Optional<UUID> findPersonInTreeByRefn(final IndexedDatabase indexedDatabase, final Refn uuidRefn) throws SQLException {
//        final Optional<UUID> optPersonGuid;
//        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
//            final RefnMap map = session.getMapper(RefnMap.class);
//            optPersonGuid = Optional.ofNullable(map.select(uuidRefn));
//            if (optPersonGuid.isPresent()) {
//                LOG.debug("located matching Person for REFN {}: PersonGUID={}", uuidRefn, optPersonGuid.get());
//            } else {
//                LOG.info("Did not find REFN {}", uuidRefn);
//            }
//        }
//        return optPersonGuid;
//    }

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

    private Document pageIndexDatabases() throws ParserConfigurationException, SQLException {
        final Document dom = XmlUtils.empty();



        final Element html = e(dom, "html");



        final Element head = e(html, "head");

        final Element title = e(head, "title");
        title.setTextContent("Databases");

        final Element css = e(head, "link");
        css.setAttribute("rel", "stylesheet");
        css.setAttribute("href", "./css/layout.css");



        final Element body = e(html, "body");

        fragNav(null, null, body);

        final Element h1 = e(body, "h3");
        h1.setTextContent("Browse a genealogy database");

        final Element ul = e(body, "ul");
        final List<IndexedDatabase> dbs = loadDatabaseIndex();
        dbs.sort((o1, o2) -> o1.file().getName().toLowerCase().compareToIgnoreCase(o2.file().getName().toLowerCase()));
        for (final IndexedDatabase iDb : dbs) {
            LOG.debug("    {}", iDb.file());
            final Element li = e(ul, "li");
            final Element link = e(li, "a");
            link.setAttribute("href", urlQueryTree(iDb));
            link.setTextContent("{"+iDb.file().getName()+"}");
        }

        fragFooter(body);

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
        final Element title = e(head, "title");
        title.setTextContent(indexedDatabase.file().getName());

        final Element css = e(head, "link");
        css.setAttribute("rel", "stylesheet");
        css.setAttribute("href", "./css/layout.css");



        final Element body = e(html, "body");

        fragNav(indexedDatabase, null, body);

        final Element h1 = e(body, "h3");
        h1.setTextContent(indexedDatabase.file().getName());

        final Element ul = e(body, "ul");
        ul.setAttribute("class", "columnar");
        for (final IndexedPerson indexedPerson : list) {
            final Element li = e(ul, "li");
            final Element ap = e(li, "a");
            ap.setAttribute("href", urlQueryTreePerson(indexedDatabase, indexedPerson));
            ap.setTextContent(indexedPerson.name());
        }

        fragFooter(body);

        return dom;
    }

    private void fragNav(final IndexedDatabase indexedDatabase, IndexedPerson indexedPerson, final Element parent) throws SQLException {
        final Element header = e(parent, "header");
        final Element nav = e(header, "nav");
        final Element a = e(nav, "a");
        a.setAttribute("href", "./");
        a.setTextContent("{home}");
        nav.appendChild(nav.getOwnerDocument().createTextNode(" "));

        if (Objects.nonNull(indexedDatabase)) {
            final Element a2 = e(nav, "a");
            a2.setAttribute("href", urlQueryTree(indexedDatabase));
            a2.setTextContent("{" + indexedDatabase.file().getName() + "} ");
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
                            final Element span = e(nav, "span");
                            span.setTextContent("see also: ");
                            labeled = true;
                        }
                        final Element a3 = e(nav, "a");
                        a3.setAttribute("href", urlQueryTreePerson(db, optPerson.get()));
                        a3.setTextContent("{" + db.file().getName() + "} ");
                    }
                }
            }

        }
    }

    private SqlSession openSessionFor(final Connection connection) {
        final SqlSessionFactory sqlSessionFactory = (SqlSessionFactory)getServletContext().getAttribute(SQL_SESSION_FACTORY);
        return sqlSessionFactory.openSession(connection);
    }

    private Document pagePerson(final IndexedDatabase indexedDatabase, final IndexedPerson indexedPerson) throws ParserConfigurationException, SQLException {
        final Person person = loadPersonDetails(indexedDatabase, indexedPerson);

        final Document dom = XmlUtils.empty();

        final Element html = e(dom, "html");



        final Element head = e(html, "head");

        final Element title = e(head, "title");
        title.setTextContent(person.nameWithSlashes());

        final Element css = e(head, "link");
        css.setAttribute("rel", "stylesheet");
        css.setAttribute("href", "./css/layout.css");



        final Element body = e(html, "body");

        fragNav(indexedDatabase, indexedPerson, body);
        e(body, "hr");
        fragPersonParents(indexedDatabase, indexedPerson, body);
        e(body, "hr");
        fragName(indexedDatabase, indexedPerson, person, body);
        e(body, "hr");

        final Footnotes<EventSource> footnotes = new Footnotes<>();
        fragEvents(indexedDatabase, new FtmLink(FtmLinkTableID.Person, person.pkid()), body, footnotes);
        fragPersonPartnerships(indexedDatabase, indexedPerson, body, footnotes);
        e(body, "hr");
        fragFootnotes(indexedDatabase, body, footnotes);
        e(body, "hr");
        fragFooter(body);

        return dom;
    }

    private void fragFootnotes(final IndexedDatabase indexedDatabase, final Element body, final Footnotes<EventSource> footnotes) {
        final int n = footnotes.size();
        if (n <= 0) {
            return;
        }

        final Element section = e(body, "section");
        section.setAttribute("class", "footnotes");
        final Element ul = e(section, "ul");

        for (int i = 1; i <= n; ++i) {
            final Element li = e(ul, "li");

            final Element footnote = e(li, "div");
            footnote.setAttributeNS(XHTML_NAMESPACE, "id", String.format("f%d", i));
            footnote.setAttribute("class", "footnote");

            final Element sup = e(footnote, "sup");
            final Element footnum = e(sup, "span");
            footnum.setAttribute("class", "footnum");
            footnum.setTextContent(formatFootnum(i, n));

            footnotes.getFootnote(i).appendTo(footnote, indexedDatabase);

            // TODO show transcripts each on their own page?
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

    private void fragEvents(final IndexedDatabase indexedDatabase, final FtmLink link, final Element body, Footnotes<EventSource> footnotes) throws SQLException {
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

        LOG.debug("EventWithSources: {}", mapEventSources);

        abbreviatePlacesOf(events);

        final Element section = e(body, "section");
        section.setAttribute("class", "events");
        final Element table = e(section, "table");
        final Element tbody = e(table, "tbody");
        for (final Event event : events) {
            final Element tr = e(tbody, "tr");
            final Element tdDate = e(tr, "td");
            tdDate.setAttribute("class", "eventDate");
            final Element monospaced = e(tdDate, "code");
            ifPresent(event.date(), monospaced);
            final Element tdPlace = e(tr, "td");
            tdPlace.setAttribute("class", "eventPlace");
            final Place place = event.place();
            if (place.isBlank()) {
                ifPresent(null, tdPlace);
            } else {
                place.appendTo(tdPlace);
            }
            final Element tdDescription = e(tr, "td");
            tdDescription.setAttribute("class", "eventDescription");
            final Element spanType = e(tdDescription, "span");
            ifPresent(event.type(), spanType);
            if (Objects.nonNull(event.description()) && !event.description().isBlank()) {
                final Element spanSep = e(tdDescription, "span");
                spanSep.setTextContent(": ");
                final Element spanDesc = e(tdDescription, "span");
                spanDesc.setTextContent(event.description());
            }

            final Optional<EventWithSources> sources = Optional.ofNullable(mapEventSources.get(event.pkid()));
            if (sources.isPresent() && !sources.get().getSources().isEmpty()) {
                sources.get().sources.forEach(s -> {
                    final int footnum = footnotes.putFootnote(s);
                    final Element sup = e(tdDescription, "sup");
                    final Element footref = e(sup, "a");
                    footref.setAttribute("class", "footref");
                    footref.setAttribute("href", "#f"+footnum);
                    footref.setTextContent("["+footnum+"]");
                });
            }
        }
    }

    private static void abbreviatePlacesOf(final List<Event> events) {
        final List<List<String>> places = events.stream().map(Event::place).map(Place::getHierarchy).collect(Collectors.toUnmodifiableList());
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
                place.setDisplay(abbrevs.get(i));
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

    private static void fragName(final IndexedDatabase indexedDatabase, final IndexedPerson indexedPerson, final Person person, final Element body) {
        final Element header = e(body, "header");
        final Element h1 = e(header, "h1");
        final Element sup = e(h1, "sup");
        final Element a = e(sup, "a");
        a.setTextContent(new String(Character.toChars(0x1F517)));
        a.setAttribute("href", urlQueryTreePerson(indexedDatabase, indexedPerson));

        final Element personName = e(h1, "span");
        personName.setAttribute("class", "personName");
        personName.setTextContent(person.nameWithSlashes());
    }

    private static void fragFooter(final Element body) {
        final Element footer = e(body, "footer");
        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        final String sNow = now.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        final Element time = e(footer, "small");
        time.setTextContent("Page generated "+sNow);
    }

    private Person loadPersonDetails(final IndexedDatabase indexedDatabase, final IndexedPerson indexedPerson) throws SQLException {
        final Person person;
        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final PersonDetailsMap map = session.getMapper(PersonDetailsMap.class);
            person = map.select(indexedPerson);
        }
        return person;
    }

    private void fragPersonParents(final IndexedDatabase indexedDatabase, final IndexedPerson indexedPerson, final Element body) throws SQLException {
        final List<PersonParent> parents;
        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final ParentsMap map = session.getMapper(ParentsMap.class);
            parents = filterParents(map.select(indexedPerson));
        }

        final Element section = e(body, "section");
        section.setAttribute("class", "parents");
        final Element table = e(section, "table");
        final Element tbody = e(table, "tbody");
        if (parents.isEmpty()) {
            final Element tr = e(tbody, "tr");
            final Element td = e(tr, "td");
            final Element span = e(td, "span");
            span.setAttribute("class", "missing");
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
                final Element nature = e(td, "span");
                if (parent.nature() != 0) { // TODO parent nature
                    nature.setAttribute("class", "nature");
                    nature.setTextContent("(" + parent.nature() + ") ");
                }
                final Element a = e(td, "a");
                a.setAttribute("href", urlQueryTreePerson(indexedDatabase, IndexedPerson.from(uuidLink)));
                a.setTextContent(parent.name());
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

    private void fragPersonPartnerships(final IndexedDatabase indexedDatabase, final IndexedPerson indexedPerson, final Element body, Footnotes<EventSource> footnotes) throws SQLException {
        final List<PersonPartnership> partnerships;

        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final PartnershipsMap map = session.getMapper(PartnershipsMap.class);
            partnerships = map.select(indexedPerson);
        }

        if (Objects.isNull(partnerships) || partnerships.isEmpty()) {
            LOG.debug("no partnerships selected");
            e(body, "hr");
            final Element section = e(body, "section");
            section.setAttribute("class", "partnership");
            final Element table = e(section, "table"); // TODO why is this a table?
            final Element tbody = e(table, "tbody");
            final Element tr = e(tbody, "tr");
            final Element td = e(tr, "td");
            final Element span = e(td, "span");
            span.setAttribute("class", "missing");
            span.setTextContent("[no known partnerships (in this database)]");
        } else {
            LOG.debug("partnerships selected: {}", partnerships);
            for (final PersonPartnership partnership : partnerships) {
                UUID uuidLink= partnership.idPerson();
                final Optional<Refn> optRefn = findRefnFor(indexedDatabase, uuidLink);
                if (optRefn.isPresent()) {
                    uuidLink = optRefn.get().uuid();
                }
                e(body, "hr");
                final Element section = e(body, "section");
                section.setAttribute("class", "partnership");
                final Element table = e(section, "table"); // TODO why is this a table?
                final Element tbody = e(table, "tbody");
                final Element tr = e(tbody, "tr");
                final Element td = e(tr, "td");
                if (partnership.nature() != 7) { //TODO partnership natures
                    final Element nature = e(td, "span");
                    nature.setAttribute("class", "nature");
                    nature.setTextContent("(" + partnership.nature() + ") ");
                }

                if (Objects.isNull(uuidLink)) {
                    final Element span = e(td, "span");
                    span.setTextContent("[unknown partner]");
                } else {
                    final Element a = e(td, "a");
                    a.setAttribute("href", urlQueryTreePerson(indexedDatabase, IndexedPerson.from(uuidLink)));
                    a.setTextContent(partnership.name());
                }

                fragEvents(indexedDatabase, new FtmLink(FtmLinkTableID.Relationship, partnership.id()), section, footnotes);

                fragPersonPartnershipChildren(indexedDatabase, partnership.id(), section);

                // TODO: how would it look if children's births were merged with partnership events?
            }
        }
    }

    private void fragPersonPartnershipChildren(final IndexedDatabase indexedDatabase, final int idRelationship, final Element section) throws SQLException {
        final List<PersonChild> children;
        try (final Connection conn = openConnectionFor(indexedDatabase); final SqlSession session = openSessionFor(conn)) {
            final ChildrenMap map = session.getMapper(ChildrenMap.class);
            children = map.select(idRelationship);
        }
        final Element table = e(section, "table");
        final Element tbody = e(table, "tbody");
        if (Objects.isNull(children) || children.isEmpty()) {
            final Element tr = e(tbody, "tr");
            final Element td = e(tr, "td");
            final Element span = e(td, "span");
            span.setAttribute("class", "missing");
            span.setTextContent("[no known children for this partnership (in this database)]");
        } else {
            for (final PersonChild child : children) {
                UUID uuidLink= child.id();
                final Optional<Refn> optRefn = findRefnFor(indexedDatabase, uuidLink);
                if (optRefn.isPresent()) {
                    uuidLink = optRefn.get().uuid();
                }
                final Element tr = e(tbody, "tr");
                final Element td = e(tr, "td");
                final Element a = e(td, "a");
                a.setAttribute("href", urlQueryTreePerson(indexedDatabase, IndexedPerson.from(uuidLink)));
                a.setTextContent(child.name()); // TODO birth dates
            }
        }
    }

    private static Connection openConnectionFor(final IndexedDatabase indexedDatabase) throws SQLException {
        final Connection conn = DriverManager.getConnection("jdbc:sqlite:"+indexedDatabase.file());
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
        return Arrays.stream(
            dirDbs.
                toFile().
                listFiles(ftmDbFilter())).
            map(IndexedDatabase::new).
            peek(IndexedDatabase::dirMedia). // <-- this peek just logs any "Media" directory names
            collect(Collectors.toList());
    }

    private static FileFilter ftmDbFilter() {
        return f -> f.isFile() && f.canRead() && f.getName().toLowerCase().endsWith(".ftm");
    }
}
