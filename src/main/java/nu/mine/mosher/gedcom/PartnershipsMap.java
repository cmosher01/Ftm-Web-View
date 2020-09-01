package nu.mine.mosher.gedcom;

import java.util.List;

public interface PartnershipsMap {
    List<PersonPartnership> select(IndexedPerson indexedPerson);
}
