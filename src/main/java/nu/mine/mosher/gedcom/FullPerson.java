package nu.mine.mosher.gedcom;

import java.util.*;

public class FullPerson {
    public Object id;
    public String nameWithSlashes;
    public ArrayList<IndexedPerson> parents = new ArrayList<>(2);

    @Override
    public String toString() {
        return this.nameWithSlashes;
    }
}
