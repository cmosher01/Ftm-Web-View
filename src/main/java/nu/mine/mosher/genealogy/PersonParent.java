package nu.mine.mosher.genealogy;

import java.util.UUID;


public record PersonParent(int i, UUID id, String name, FtmNature nature, int grandparents) {
}
