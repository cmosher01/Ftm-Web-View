package nu.mine.mosher.genealogy;

import java.util.UUID;

public interface RefnReverseMap {
    Refn select(UUID personGuid);
}
