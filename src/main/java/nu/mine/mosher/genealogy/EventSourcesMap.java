package nu.mine.mosher.genealogy;



import java.util.List;



public interface EventSourcesMap {
    List<EventWithSources> select(FtmLink link);
}
