package nu.mine.mosher.gedcom;

import java.util.List;

public interface ChildrenMap {
    record ParentRel(int pkidRelationship, int pkidParentPerson){}

    List<PersonChild> select(ParentRel rel);
}
