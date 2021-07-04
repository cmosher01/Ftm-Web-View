package nu.mine.mosher.genealogy;



import java.util.List;



public interface EventsMap {
    List<Event> select(FtmLink link);
}
