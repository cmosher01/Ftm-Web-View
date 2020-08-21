/*
    Ftm-Web-View
    Web server for Family Tree Maker SQLite databases

    Copyright © 2020, by Christopher Alan Mosher, Shelton, Connecticut, USA, cmosher01@gmail.com .

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/


package nu.mine.mosher.gedcom;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.sql.rowset.RowSetProvider;
import javax.sql.rowset.WebRowSet;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.JulianFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;



public class FtmWebView {
    private static final Logger LOG;
    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.FINEST);
        LOG = LoggerFactory.getLogger(FtmWebView.class);
    }

    public static void main(final String... args) throws SQLException, IOException {
        LOG.debug("starting: {}", FtmWebView.class.getName());

        final List<IndexedDatabase> dbs = loadDatabaseIndex();
        dbs.forEach(System.out::println);

//        rowsetTest(db);

        final List<IndexedPerson> rPerson = new ArrayList<>();
        loadPeopleIndex(dbs.get(0), rPerson);
        rPerson.forEach(System.out::println);

//        final Map<Integer, Note> notes = new HashMap<>();
//        loadNotes(db, new IndexedPerson(15, ""), notes);

//        final Map<Integer, Event> events = new HashMap<>();
//        final Map<Integer, Citation> citations = new HashMap<>();
//        final Map<Integer, Note> notes = new HashMap<>();
//        loadEvents(db, new IndexedPerson(15, ""), events, citations, notes);
//        events.forEach((k,v) -> LOG.debug("ID={}, {}",k,v));
//        citations.forEach((k,v) -> LOG.debug("ID={}, {}",k,v));
//        notes.forEach((k,v) -> LOG.debug("ID={}, {}",k,v));



//        inspectFactDates(db);



        System.out.flush();
        System.err.flush();
    }

    private static List<IndexedDatabase> loadDatabaseIndex() {
        return Arrays.stream(
            Paths.get("").
            toAbsolutePath().
            normalize().
            toFile().
            listFiles(ftmDbFilter())).
            map(IndexedDatabase::new).
            collect(Collectors.toList());
    }

    private static FileFilter ftmDbFilter() {
        return f -> f.isFile() && f.canRead() && f.getName().toLowerCase().endsWith(".db");
    }

    private static void rowsetTest(IndexedDatabase idb) throws SQLException, IOException {
        try (final WebRowSet rs = RowSetProvider.newFactory().createWebRowSet()) {
            rs.setUrl("jdbc:sqlite:"+idb.file());
            rs.setEscapeProcessing(false);
            rs.setCommand("SELECT ID, FullNameReversed FROM Person");
            rs.execute();
            rs.writeXml(System.out);
        }
    }

    private static void loadNotes(IndexedDatabase idb, IndexedPerson person, Map<Integer, Note> notes) throws SQLException, IOException {
        try (final Connection db = DriverManager.getConnection("jdbc:sqlite:"+idb.file())) {
            try (final PreparedStatement statement = db.prepareStatement(getRes("sql/notes.sql"))) {
                statement.setInt(1, FtmLinkTableID.Person.id());
                statement.setInt(2, person.id());
                try (final ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        notes.put(rs.getInt("id"), ResultSetConverter.buildNote(rs));
                    }
                }
            }
        }
    }

    private static void inspectFactDates(final IndexedDatabase idb) throws SQLException {
        try (
            final Connection db = DriverManager.getConnection("jdbc:sqlite:"+idb.file());
            final Statement statement = db.createStatement();
            final ResultSet rs = statement.executeQuery("SELECT Date FROM Fact")) {
            while (rs.next()) {
                final int d = rs.getInt("Date");
                if (!rs.wasNull()) {
                    final int n = d >> 9;
                    final LocalDate date = toLocalDate(n);

                    final int f = d & 0x1FF;
                    LOG.debug("n={} --> {} , f={}",
                        n,
                        date.format(DateTimeFormatter.ISO_DATE),
                        Integer.toBinaryString(f));
                }
            }
        }
    }

    private static LocalDate toLocalDate(final int n) {
        return LocalDate.MIN.with(JulianFields.JULIAN_DAY, n);
    }

    public static void loadPeopleIndex(final IndexedDatabase idb, final Collection<IndexedPerson> addTo) throws SQLException {
        try (
            final Connection db = DriverManager.getConnection("jdbc:sqlite:"+idb.file());
            final Statement statement = db.createStatement();
            final ResultSet rs = statement.executeQuery("SELECT ID, FullNameReversed FROM Person")) {
            while (rs.next()) {
                addTo.add(new IndexedPerson(rs.getInt("ID"), rs.getString("FullNameReversed")));
            }
        }
    }

    public static void loadEvents(
        final IndexedDatabase idb,
        final IndexedPerson person,
        final Map<Integer, Event> events,
        final Map<Integer, Citation> citations,
        final Map<Integer, Note> notes
        ) throws SQLException, IOException {
        try (final Connection db = DriverManager.getConnection("jdbc:sqlite:"+idb.file())) {
            loadFacts(db, person, events);
            loadFactSources(db, person, events, citations);
            loadFactNotes(db, person, events, notes);
        }
    }

    private static void loadFactNotes(Connection db, IndexedPerson person, Map<Integer, Event> events, Map<Integer, Note> notes) throws SQLException, IOException {
        try (final PreparedStatement statement = db.prepareStatement(getRes("sql/fact_notes.sql"))) {
            statement.setInt(1, FtmLinkTableID.Person.id());
            statement.setInt(2, person.id());
            try (final ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    final int id = rs.getInt("id_note");
                    if (0 < id) {
                        final Note note = notes.computeIfAbsent(id, k -> ResultSetConverter.buildNote(rs));
                        final Event event = events.get(rs.getInt("id_fact"));
                        if (Objects.isNull(event)) {
                            throw new IllegalStateException("event not found: " + rs.getInt("id"));
                        }
                        event.notes().add(note);
                    }
                }
            }
        }
    }

    private static void loadFacts(Connection db, IndexedPerson person, Map<Integer, Event> events) throws SQLException, IOException {
        try (final PreparedStatement statement = db.prepareStatement(getRes("sql/facts.sql"))) {
            statement.setInt(1, FtmLinkTableID.Person.id());
            statement.setInt(2, person.id());
            try (final ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    events.put(rs.getInt("id"), ResultSetConverter.buildEvent(rs));
                }
            }
        }
    }

    private static void loadFactSources(Connection db, IndexedPerson person, Map<Integer, Event> events, Map<Integer, Citation> citations) throws SQLException, IOException {
        try (final PreparedStatement statement = db.prepareStatement(getRes("sql/fact_sources.sql"))) {
            statement.setInt(1, FtmLinkTableID.Person.id());
            statement.setInt(2, person.id());
            try (final ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    final int id = rs.getInt("id_citation");
                    if (0 < id) {
                        final Citation cita = citations.computeIfAbsent(id, k -> ResultSetConverter.buildCitation(rs));
                        final Event event = events.get(rs.getInt("id_fact"));
                        if (Objects.isNull(event)) {
                            throw new IllegalStateException("event not found: " + rs.getInt("id"));
                        }
                        event.citations().add(cita);
                    }
                }
            }
        }
    }

    private static String getRes(final String fileName) throws IOException {
        try (final InputStream is = FtmWebView.class.getResourceAsStream(fileName)) {
            return new String(is.readAllBytes(), StandardCharsets.US_ASCII);
        }
    }
}
