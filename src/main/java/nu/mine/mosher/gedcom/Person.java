package nu.mine.mosher.gedcom;

import java.util.*;

public record Person(UUID id, String nameWithSlashes, int pkid, Timestamp lastmod) {
}
