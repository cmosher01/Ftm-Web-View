package nu.mine.mosher.gedcom;

import java.util.UUID;

public interface RefnReverseMap {
    Refn select(UUID personGuid);
}
