package nu.mine.mosher.gedcom;

import java.util.List;

public interface ParentsMap {
    List<PersonParent> select(IndexedPerson indexedPerson);
}
