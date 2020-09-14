package nu.mine.mosher.gedcom;



import java.util.List;



public interface EventSourcesMap {
    List<EventWithSources> select(FtmLink link);
}
