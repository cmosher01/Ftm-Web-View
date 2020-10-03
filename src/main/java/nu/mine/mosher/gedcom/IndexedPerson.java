package nu.mine.mosher.gedcom;

import java.util.*;


public record IndexedPerson(UUID id, Refn refn, String name, int pkid, Day dateBirth) implements Comparable<IndexedPerson> {
    public static IndexedPerson from(final UUID uuidPerson) {
        return new IndexedPerson(uuidPerson, new Refn(uuidPerson), null, 0, Day.UNKNOWN);
    }

    @Override
    // not consistent with equals
    public int compareTo(final IndexedPerson that) {
        return this.name().compareToIgnoreCase(that.name());
    }

    public UUID preferRefn() {
        if (Objects.nonNull(refn())) {
            return refn().uuid();
        }
        return id();
    }

    public boolean isRecent() {
        return Objects.nonNull(dateBirth) && dateBirth.isRecent();
    }
}
