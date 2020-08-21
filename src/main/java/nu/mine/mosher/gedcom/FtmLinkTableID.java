package nu.mine.mosher.gedcom;



/**
 * FtmLinkTableID refers to tables, as defined in FTM.Data.DB.dll: FTM.Data.DB/TableID enum.
 */
public enum FtmLinkTableID {
    Fact(2),
    Person(5),
    Relationship(7),
    Task(9),
    MediaFile(13),
    Source(16);

    private final int id;

    FtmLinkTableID(final int id) {
        this.id = id;
    }

    public int id() {
        return this.id;
    }
}
