package nu.mine.mosher.genealogy;

import java.util.*;


public record IndexedPerson(UUID id, Refn refn, String name, int pkid, Day dateBirth, Day dateDeath) implements Comparable<IndexedPerson> {
    public static IndexedPerson from(final UUID uuidPerson) {
        return new IndexedPerson(uuidPerson, new Refn(uuidPerson), null, 0, Day.UNKNOWN, Day.UNKNOWN);
    }

    @Override
    // not consistent with equals
    public int compareTo(final IndexedPerson that) {
        return Comparator.
             comparing(IndexedPerson::name).
             thenComparing(IndexedPerson::dateBirth).
             thenComparing(IndexedPerson::dateDeath).
             compare(this, that);
    }

    public UUID preferRefn() {
        if (Objects.nonNull(refn())) {
            return refn().uuid();
        }
        return id();
    }

    public boolean isRecent() {
        return Objects.nonNull(this.dateBirth) && this.dateBirth.isRecent();
    }

    public String dates() {
        return this.dateBirth.simplistic()+"-"+this.dateDeath.simplistic();
    }
}
