package nu.mine.mosher.genealogy;

import org.jspecify.annotations.NonNull;

import java.util.*;


public record IndexedPerson(
    UUID id,
    Refn refn,
    String name,
    int pkid,
    Day dateBirth,
    Day dateDeath,
    String xy,
    String gedcomname,
    String sex,
    // TODO add Place.DisplayName
    FtmPlace birthplace,
    FtmPlace anyplace
) implements Comparable<IndexedPerson> {
    @NonNull
    public static IndexedPerson from(final UUID uuidPerson) {
        return new IndexedPerson(uuidPerson, new Refn(uuidPerson),
        null, 0, Day.UNKNOWN, Day.UNKNOWN, null, null, null, null, null);
    }

    @Override
    // not consistent with equals
    public int compareTo(@NonNull final IndexedPerson that) {
        return Comparator.
            comparing(IndexedPerson::name, String::compareToIgnoreCase).
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

    @NonNull
    public String dates() {
        return this.dateBirth.simplistic()+"-"+this.dateDeath.simplistic();
    }
}
