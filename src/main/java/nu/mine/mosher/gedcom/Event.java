package nu.mine.mosher.gedcom;



import java.util.List;



//public record Event(int pkid, Day date, Place place, String type, String description, List<Citation> citations, List<Note> notes) implements Comparable<Event> {
public record Event(int pkid, Day date, String place, String type, String description/*, List<Citation> citations, List<Note> notes*/) implements Comparable<nu.mine.mosher.gedcom.Event> {
    @Override
    // not consistent with equals
    public int compareTo(Event that) {
        return this.date().compareTo(that.date());
    }
}
