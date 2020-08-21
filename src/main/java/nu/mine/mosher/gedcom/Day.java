package nu.mine.mosher.gedcom;



import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.temporal.JulianFields;



public record Day(int ftmDate, LocalDate date) implements Comparable<Day> {
    public static Day fromFtmFactDate(final int dateFtmFact) {
        return new Day(dateFtmFact, LocalDate.MIN.with(JulianFields.JULIAN_DAY, dateFtmFact >> 9));
    }

    @Override
    // not consistent with equals
    public int compareTo(@NotNull Day that) {
        return this.date().compareTo(that.date());
    }
}
