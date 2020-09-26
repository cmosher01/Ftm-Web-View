package nu.mine.mosher.gedcom;



public record Event(int pkid, Day date, Place place, String type, String description) implements Comparable<Event> {
    @Override
    // not consistent with equals
    public int compareTo(Event that) {
        return this.date().compareTo(that.date());
    }
}
