package nu.mine.mosher.gedcom;



import java.util.ArrayList;
import java.util.List;



public class EventWithSources {
    public int pkidFact;
    public List<EventSource> sources = new ArrayList<>();

    public void setPkidFact(int pkidFact) {
        this.pkidFact = pkidFact;
    }

    public List<EventSource> getSources() {
        return this.sources;
    }

    @Override
    public String toString() {
        return "Event.ID=" + this.pkidFact + ", sources=" + this.sources + '}';
    }
}
