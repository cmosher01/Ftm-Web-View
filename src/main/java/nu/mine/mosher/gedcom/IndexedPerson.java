package nu.mine.mosher.gedcom;



import org.jetbrains.annotations.NotNull;



public record IndexedPerson(int id, String nameSort) implements Comparable<IndexedPerson> {
    @Override
    // not consistent with equals
    public int compareTo(@NotNull IndexedPerson that) {
        return this.nameSort().compareToIgnoreCase(that.nameSort());
    }
}
