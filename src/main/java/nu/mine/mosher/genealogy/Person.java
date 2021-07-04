package nu.mine.mosher.genealogy;

import java.util.UUID;

public record Person(UUID id, String nameWithSlashes, int pkid, Timestamp lastmod) {
}
