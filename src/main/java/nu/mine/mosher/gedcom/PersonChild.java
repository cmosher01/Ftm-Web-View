package nu.mine.mosher.gedcom;

import java.util.UUID;


public record PersonChild(UUID id, String name, FtmNature nature, Day dateBirth, int grandchildren) {
}
