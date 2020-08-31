package nu.mine.mosher.gedcom;

import java.util.List;

public interface ChildrenMap {
    List<PersonChild> select(int idRelationship);
}
