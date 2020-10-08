package nu.mine.mosher.gedcom;


import org.slf4j.*;

import java.util.Objects;

import static nu.mine.mosher.gedcom.StringUtils.safe;

// TODO links for "FamilySearch ID", "WikiTree ID", etc. For others, see: https://en.wikipedia.org/wiki/List_of_genealogy_databases
// TODO style Name
public record Event(int pkid, Day date, Place place, String type, String description) implements Comparable<Event> {
    private static final Logger LOG =  LoggerFactory.getLogger(Event.class);

    @Override
    // not consistent with equals
    public int compareTo(Event that) {
        return this.date().compareTo(that.date());
    }

    public String description() {
        return safe(type()).equalsIgnoreCase("sex") ? parseSex(description) : this.description;
    }

    private String parseSex(final String description) {
        try {
            return switch (Integer.parseInt(description)) {
                case 0 -> "male";
                case 1 -> "female";
                default -> "[unknown]";
            };
        } catch (final Throwable e) {
            LOG.warn("invalid value for sex type: {}", description, e);
        }
        return "[unknown]";
    }

    public boolean isRecent() {
        return Objects.nonNull(date) && date.isRecent();
    }
}
