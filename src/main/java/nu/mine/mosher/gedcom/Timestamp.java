package nu.mine.mosher.gedcom;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class Timestamp {
    private final ZonedDateTime t;

    public Timestamp(final long epochSecond) {
        this.t = ZonedDateTime.of(LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC), ZoneOffset.UTC);
    }

    @Override
    public String toString() {
        return t.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }
}
