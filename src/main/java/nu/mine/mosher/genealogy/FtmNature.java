package nu.mine.mosher.genealogy;

import java.util.Arrays;

public enum FtmNature {
    Biological(0),
    Adopted(1),
    Step(2),
    Foster(3),
    Related(4),
    Guardian(5),
    Sealed(6),
    Married(7),
    Partner(8),
    Friend(9),
    Single(10),
    Private(11),
    Other(12),
    Unknown(13);

    private final int id;

    FtmNature(final int id) {
        this.id = id;
    }

    public boolean display() {
        return !(this.equals(Biological) || this.equals(Married));
    }

    public static FtmNature fromId(final int id) {
        return Arrays.stream(values()).filter(v -> v.id == id).findAny().orElseThrow();
    }
}
