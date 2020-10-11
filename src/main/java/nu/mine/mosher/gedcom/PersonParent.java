package nu.mine.mosher.gedcom;

import java.util.UUID;


public record PersonParent(int i, UUID id, String name, FtmNature nature, int grandparents) {
}
