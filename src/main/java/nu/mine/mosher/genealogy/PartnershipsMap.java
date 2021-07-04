package nu.mine.mosher.genealogy;

import java.util.List;

public interface PartnershipsMap {
    List<PersonPartnership> select(IndexedPerson indexedPerson);
}
