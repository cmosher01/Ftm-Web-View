package nu.mine.mosher.gedcom;

import java.util.UUID;

public record Refn(UUID uuid) {
    public String getId() {
        return this.uuid().toString();
    }
}
