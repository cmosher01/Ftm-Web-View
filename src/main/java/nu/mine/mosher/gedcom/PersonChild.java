package nu.mine.mosher.gedcom;

import java.util.UUID;


public record PersonChild(UUID id, String name, int nature) {
}
