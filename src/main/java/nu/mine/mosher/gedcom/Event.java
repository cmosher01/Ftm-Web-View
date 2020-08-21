package nu.mine.mosher.gedcom;



import org.jetbrains.annotations.NotNull;

import java.util.List;



public record Event(Day date, Place place, String type, String description, List<Citation> citations, List<Note> notes) implements Comparable<Event> {
    @Override
    // not consistent with equals
    public int compareTo(@NotNull Event that) {
        return this.date().compareTo(that.date());
    }
}
