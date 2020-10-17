package nu.mine.mosher.gedcom;


import org.slf4j.*;

import java.util.*;


// TODO style Name
public record Event(int pkid, Day date, Place place, FtmFactTypeTag tag, String type, String description) implements Comparable<Event> {
    private static final Logger LOG =  LoggerFactory.getLogger(Event.class);

    public String description() {
        return FtmFactTypeTag.SEX == tag() ? parseSex(this.description) : this.description;
    }

    @Override
    // not consistent with equals
    public int compareTo(Event that) {
        return this.date().compareTo(that.date());
    }

    private static String parseSex(final String description) {
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
        return Objects.nonNull(date()) && date().isRecent();
    }

    public boolean isPrimary() {
        return Objects.nonNull(tag()) && FtmFactTypeTag.setPrimary.contains(tag());
    }

    public boolean isSecondary() {
        return Objects.nonNull(tag()) && FtmFactTypeTag.setSecondary.contains(tag());
    }

    public Optional<WorldTreeID> getIfID() {
        if (type().length() < 3) {
            return Optional.empty();
        }
        if (!type().substring(type().length()-3).equalsIgnoreCase(" ID")) {
            return Optional.empty();
        }
        try {
            return Optional.of(WorldTreeID.valueOf(type().substring(0, type().length() - 3).toUpperCase()));
        } catch (final Throwable e) {
            return Optional.empty();
        }
    }
}
