package nu.mine.mosher.gedcom;

import java.util.*;


public record IndexedPerson(UUID id, Refn refn, String name) implements Comparable<IndexedPerson> {
    public static IndexedPerson from(final UUID uuidPerson) {
        return from(uuidPerson, true);
    }

    public static IndexedPerson from(final UUID uuidPerson, boolean withRefn) {
        return new IndexedPerson(uuidPerson, withRefn ? new Refn(uuidPerson) : null, null);
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
}
