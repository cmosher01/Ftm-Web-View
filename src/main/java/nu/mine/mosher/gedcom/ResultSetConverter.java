package nu.mine.mosher.gedcom;



import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.util.ArrayList;



public final class ResultSetConverter {
    private ResultSetConverter() {
    }

    @NotNull
    public static Event buildEvent(final ResultSet rs) {
        try {
            return new Event(
                Day.fromFtmFactDate(rs.getInt("ftm_date")),
                new Place(rs.getString("ftm_place")),
                rs.getString("type"),
                rs.getString("description"),
                new ArrayList<>(),
                new ArrayList<>());
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public static Citation buildCitation(final ResultSet rs) {
        try {
            return new Citation(
                rs.getString("author"),
                rs.getString("title"),
                rs.getString("place_pub"),
                rs.getString("pub"),
                rs.getString("date_pub"),
                rs.getString("citation"));
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public static Note buildNote(final ResultSet rs) {
        try {
            return new Note(
                rs.getString("note"));
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
