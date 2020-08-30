package nu.mine.mosher.gedcom;

import java.util.UUID;

public interface RefnReverseMap {
    String select(UUID personGuid);
}
