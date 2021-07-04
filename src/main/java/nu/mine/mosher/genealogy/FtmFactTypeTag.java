package nu.mine.mosher.genealogy;

import java.util.Set;

public enum FtmFactTypeTag {
    NAME,
    SEX,
    BIRT,
    DEAT,
    MARR,
    DIV,
    ANUL,
    CHR,
    BAPM,
    BURI,
    CREM,
    RESI,
    CENS,
    PROB,
    WILL,
    MARB,
    MARC,
    MARL,
    MARS;

    public static final Set<FtmFactTypeTag> setPrimary = Set.of(BIRT, DEAT, MARR, DIV, ANUL, NAME, SEX);
    public static final Set<FtmFactTypeTag> setSecondary = Set.of(CHR, BAPM, BURI, CREM, RESI, CENS, PROB, WILL, MARB, MARC, MARL, MARS);
}
