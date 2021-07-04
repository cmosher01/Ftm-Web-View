package nu.mine.mosher.genealogy;

import java.util.UUID;

public record Refn(UUID uuid) {
    public String getId() {
        return this.uuid().toString();
    }
}
