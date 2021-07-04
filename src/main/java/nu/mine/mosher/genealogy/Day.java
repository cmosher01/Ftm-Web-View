package nu.mine.mosher.genealogy;


import org.slf4j.*;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.JulianFields;
import java.util.Objects;
import java.util.regex.*;

import static nu.mine.mosher.genealogy.DatabaseUtil.bits;



public class Day implements Comparable<Day> {
    private static final Logger LOG =  LoggerFactory.getLogger(Day.class);
    private static final FlaggedDate FD_UNKNOWN = new FlaggedDate(0x80000011);

    private final FlaggedDate earliest;
    private final FlaggedDate latest;
    private final String other;

    public static final Day UNKNOWN = new Day(FD_UNKNOWN, FD_UNKNOWN);

    private Day(final long d1, final long d2, final String other) {
        FlaggedDate fd1 = new FlaggedDate(d1);
        FlaggedDate fd2 = new FlaggedDate(d2);

        // doctor up "after/before" flags into earliest/latest dates
        if (fd1.equals(fd2) && !fd1.about) {
            if (fd1.after) {
                fd2 = FD_UNKNOWN;
            } else if (fd1.before) {
                fd2 = fd1;
                fd1 = FD_UNKNOWN;
            }
        }

        this.earliest = fd1;
        this.latest = fd2;
        this.other = other;

        dump();
    }

    private Day(FlaggedDate fd1, FlaggedDate fd2) {
        this.earliest = fd1;
        this.latest = fd2;
        this.other = "";
    }

    @Override
    public String toString() {
        if (this.other.isEmpty()) {
            final StringBuilder s = new StringBuilder(32);
            s.append(this.earliest);
            if (!this.latest.equals(this.earliest)) {
                s.append('~');
                s.append(this.latest);
            }
            return s.toString();
        } else {
            return String.format("\u201C%s\u201D", this.other);
        }
    }

    public void dump(){
        if (!other.isBlank()) {
            LOG.trace("Day (non-parsed): \"{}\"", this.other);
        } else {
            if (this.earliest.equals(this.latest)) {
                LOG.trace("Day: {}", this.earliest.dump());
            } else {
                LOG.trace("Day: earliest: {}", this.earliest.dump());
                LOG.trace("Day:   latest: {}", this.latest.dump());
            }
        }
    }

    private static final Pattern ONE_DATE = Pattern.compile("^(\\d+)$");
    private static final Pattern TWO_DATES = Pattern.compile("^(\\d+):(\\d+)$");

    public static Day fromFtmFactDate(final String d) {
        Matcher m;
        if ((m = ONE_DATE.matcher(d)).matches()) {
            final long date = Long.parseLong(m.group(1), 10);
            return new Day(date, date, "");
        } else if ((m = TWO_DATES.matcher(d)).matches()) {
            final long date1 = Long.parseLong(m.group(1), 10);
            final long date2 = Long.parseLong(m.group(2), 10);
            return new Day(date1, date2, "");
        } else {
            return new Day(0, 0, d);
        }
    }

    @Override
    // not consistent with equals
    public int compareTo(Day that) {
        return this.earliest.compareTo(that.earliest);
    }

    public boolean isRecent() {
        return this.latest.isRecent() || this.earliest.isRecent();
    }


    private static class FlaggedDate implements Comparable<FlaggedDate> {
        private final long flags;
        private final boolean unknown;
        private final long d;
        private final LocalDate ld;
        private final boolean before;
        private final boolean after;
        private final boolean about;
        private final boolean dualYear;
        private final boolean noYear;
        private final boolean noMonth;
        private final boolean noDay;
        private final boolean calculated;

        private FlaggedDate(final long n) {
            this.flags = n & 0x1FF;
            this.unknown = (Integer.MIN_VALUE & n) != 0;
            this.d = (Integer.MAX_VALUE & n) >> 9;

            this.ld = LocalDate.MIN.with(JulianFields.JULIAN_DAY, this.d);
            final BigInteger f = BigInteger.valueOf(this.flags);
            this.before = f.testBit(0);
            this.after = f.testBit(1);
            this.about = this.before && this.after;
            this.dualYear = f.testBit(4);
            this.noYear = f.testBit(5);
            this.noMonth = f.testBit(6);
            this.noDay = f.testBit(7);
            this.calculated = f.testBit(8);
        }

        @Override
        public String toString() {
            // TODO XML string (to dim unknown date parts)
            if (this.unknown) {
                return "\u00d7\u00d7\u00d7\u00d7\u2012\u00d7\u00d7\u2012\u00d7\u00d7";
            }

            final StringBuilder s = new StringBuilder(16);
            if (this.noYear) {
                s.append("\u00d7".repeat(4)); // math cross product/multiply symbol: "x"
            } else {
                int year = this.ld.getYear();
                if (year < 0)
                {
                    year = 1-year;
                    s.append('\u2212'); // math minus/negative: "-"
                }
                s.append(String.format("%4d", year).replace(' ', '\u00A0'));
            }
            s.append('\u2012'); // figure dash
            if (this.noMonth) {
                s.append("\u00d7".repeat(2)); // math cross product/multiply symbol: "x"
            } else {
                int month = this.ld.getMonthValue();
                s.append(String.format("%02d", month));
            }
            s.append('\u2012'); // figure dash
            if (this.noDay) {
                s.append("\u00d7".repeat(2)); // math cross product/multiply symbol: "x"
            } else {
                int day = this.ld.getDayOfMonth();
                s.append(String.format("%02d", day));
            }
//            s.append('[');
//            s.append(this.after ? 'A' : '-');
//            s.append(this.before ? 'B' : '-');
//            s.append(this.dualYear ? '/' : '-');
//            s.append(this.calculated ? '=' : '-');
//            s.append(']');
            if (this.about) {
                s.append("~");
            }
            if (this.calculated) {
                s.append("=");
            }

            return s.toString();
        }

        public String dump() {
            return "date="+String.format("\"{%s}(%d)%s[%9s]\"",
                unknown ? '1' : '0',
                d,
                ld.format(DateTimeFormatter.ISO_LOCAL_DATE),
                bits(flags, 9))
                +", fmt=\""+this+"\"";
        }

        @Override
        // not consistent with equals
        public int compareTo(final Day.FlaggedDate o) {
            return Long.compare(this.d, o.d);
        }

        @Override
        public boolean equals(Object o) {
            if (Objects.isNull(o) || !(o instanceof final FlaggedDate that)) {
                return false;
            }
            return this.flags == that.flags && this.unknown == that.unknown && this.d == that.d;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.flags, this.unknown, this.d);
        }

        // TODO parameterize years for recency?
        // TODO implement privatization based on database columns in tables:
        // Person, Relationship, ChildRelationship, Fact, Note, MediaLink, MediaFile
        public boolean isRecent() {
            return  !this.unknown && LocalDate.now().minusYears(110).compareTo(this.ld) < 0;
        }
    }
}
