package nu.mine.mosher.gedcom;

import java.util.*;

import static nu.mine.mosher.gedcom.DatabaseUtil.*;


public record IndexedPerson(Object id, String name) implements Comparable<IndexedPerson> {
    public IndexedPerson {
        if (!(Objects.requireNonNull(id) instanceof byte[])) {
            throw new IllegalArgumentException("id of unexpected type: "+id.getClass().getCanonicalName());
        }
        Objects.requireNonNull(name);
    }

    public static IndexedPerson from(final UUID uuidPerson) {
        return new IndexedPerson(permute(bytesOf(uuidPerson)), "");
    }

    @Override
    // not consistent with equals
    public int compareTo(final IndexedPerson that) {
        return this.name().compareToIgnoreCase(that.name());
    }

    public UUID uuid() {
        return uuidOf(permute(id()));
    }

    public byte[] getId() {
        final byte[] s = (byte[])id();
        final byte[] d = new byte[16];
        System.arraycopy(s, 0, d, 0, 16);
        return d;
    }
}
