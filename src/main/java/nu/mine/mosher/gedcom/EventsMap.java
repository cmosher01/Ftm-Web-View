package nu.mine.mosher.gedcom;



import java.util.List;



public interface EventsMap {
    List<Event> select(FtmLink link);
}
