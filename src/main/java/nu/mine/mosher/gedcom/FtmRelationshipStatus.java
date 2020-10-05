package nu.mine.mosher.gedcom;

import java.util.Arrays;

public enum FtmRelationshipStatus {
    Unknown(0),
    Ongoing(1),
    Divorced(2),
    Deceased(3),
    Separated(4),
    Annulled(5),
    Private(6),
    Other(7),
    None(8);

    private final int id;

    FtmRelationshipStatus(final int id) {
        this.id = id;
    }

    public boolean display() {
        return !(this.equals(Ongoing) || this.equals(Unknown));
    }

    public static FtmRelationshipStatus fromId(final int id) {
        return Arrays.stream(values()).filter(v -> v.id == id).findAny().orElseThrow();
    }
}
