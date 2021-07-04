package nu.mine.mosher.genealogy;

import java.util.UUID;


public record PersonChild(UUID id, String name, FtmNature nature, Day dateBirth, int grandchildren) {
}
