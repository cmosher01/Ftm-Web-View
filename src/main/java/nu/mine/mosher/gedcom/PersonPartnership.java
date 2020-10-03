package nu.mine.mosher.gedcom;

import java.util.*;


public record PersonPartnership(int id, UUID idPerson, String name, int nature, Day dateSort) {
    public boolean isRecent() {
        return Objects.nonNull(dateSort()) && dateSort().isRecent();
    }
}
