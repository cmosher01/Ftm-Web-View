package nu.mine.mosher.gedcom;

import java.util.*;


public record PersonPartnership(int id, UUID idPerson, FtmRelationshipStatus status, String name, FtmNature nature, Day dateSort) {
    public boolean isRecent() {
        return Objects.nonNull(dateSort()) && dateSort().isRecent();
    }
}
