package nu.mine.mosher.gedcom;

import java.util.UUID;


public record PersonParent(UUID id, String name, int nature) {
}
