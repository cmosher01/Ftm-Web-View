package nu.mine.mosher.genealogy;

import java.util.List;

public interface ParentsMap {
    List<PersonParent> select(IndexedPerson indexedPerson);
}
