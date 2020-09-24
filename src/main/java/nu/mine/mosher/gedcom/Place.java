package nu.mine.mosher.gedcom;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;



public class Place {
    private static final Logger LOG =  LoggerFactory.getLogger(Place.class);

    private final List<String> hierarchy;
    private List<String> display;
    private String sDisplay = "";
    private final String description;
    private final double radLat;
    private final double radLon;
    private final boolean neg;
    private final int codeStandard;
    private boolean ditto;

    private Place(List<String> hierarchy, String description, double radLat, double radLon, boolean neg, int codeStandard) {
        this.hierarchy = List.copyOf(hierarchy);
        this.display = new ArrayList<>(hierarchy);
        this.description = description;
        this.radLat = radLat;
        this.radLon = radLon;
        this.neg = neg;
        this.codeStandard = codeStandard;
    }

    public List<String> getHierarchy() {
        return new ArrayList<>(this.hierarchy);
    }

    public static Place fromFtmPlace(final String s) {
        final Place place = new Builder(s).build();
        LOG.debug("FtmPlace=\"{}\" --> \"{}\"", s, place.toString());
        return place;
    }

    @Override
    public String toString() {
        if (this.ditto) {
            return "\u00A0\u201D";
        }

        StringBuilder s = new StringBuilder(100);
        s.append(this.sDisplay.isBlank() ? this.description : this.sDisplay);

        if (this.radLat != 0.0d || this.radLon != 0.0d) {
            s.append(String.format(" (%09.7f,%09.7f)", this.radLat, this.radLon));
        }

        if (this.neg) {
            s.append(" *");
        }

        if (this.codeStandard != 0) {
            s.append(String.format(" [%d]", this.codeStandard));
        }

        return s.toString();
    }

    public void setDisplay(final List<String> display) {
        this.display = new ArrayList<>(display);
        this.sDisplay = String.join(", ", this.display);
    }

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof Place)) {
            return false;
        }
        final Place that = (Place) object;
        return this.hierarchy.equals(that.hierarchy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.hierarchy);
    }

    public void setDitto() {
        this.ditto = true;
    }

    private static class Builder {
        private List<String> hierarchy;
        private String description;
        private double radLat;
        private double radLon;
        private boolean neg;
        private int codeStandard;

        public Builder(final String description) {
            // default value if any parsing fails:
            this.description = String.format("\u201C%s\u201D", description);
            this.hierarchy = Collections.emptyList();

            try {
                parseDescription(description);
            } catch (final Throwable e) {
                LOG.warn("unknown place name format for {}", description, e);
            }
        }

        private void parseDescription(String description) {
            if (description.isBlank()) {
                this.description = "";
                return;
            }

            // TODO: /another place / with slashes | and  bars, but, resolved, in///Connecticut/USA/-9//
            // TODO: /Place, Name w/some slash/es | and, verical | bars|//
            if (description.contains("|")) {
                /*
                        /Hamilton, Madison, New York, USA|/0.7474722/-1.318502
                 */
                final String[] s = description.split("\\|", 2);
                parseCodes(s[1].split("/", -1));
                String d = s[0];
                if (d.startsWith("/")) {
                    d = d.substring(1);
                }
                this.description = d;
            } else {
                /*
                        /Room 401, Flint Hall, Syracuse University/Syracuse/Onondaga/New York/USA/11269/0.7513314/-1.329023
                 */
                final String[] s = description.split("/", -1);
                parseCodes(s);
                final StringBuilder sb = new StringBuilder(100);
                for (int i = 0; i < s.length-3; ++i) {
                    if (!s[i].isBlank()) {
                        if (0 < sb.length()) {
                            sb.append(", ");
                        }
                        sb.append(s[i]);
                    }
                }
                this.description = sb.toString();
            }
            this.hierarchy =
                Arrays.stream(this.description.split(",")).
                map(String::trim).
                collect(Collectors.toUnmodifiableList());
        }

        private void parseCodes(final String[] s) throws NumberFormatException, ArrayIndexOutOfBoundsException {
            this.radLat = parseRadians(s[s.length-2]);
            this.radLon = parseRadians(s[s.length-1]);

            this.codeStandard = parseCode(s[s.length-3]);

            this.neg = false;
            if (this.codeStandard < 0) {
                this.neg = true;
                this.codeStandard = -this.codeStandard;
            }
        }

        private static double parseRadians(final String s) {
            if (s.isBlank()) {
                return 0.0d;
            }
            return Double.parseDouble(s);
        }

        private static int parseCode(final String s) {
            if (s.isBlank()) {
                return 0;
            }
            return Integer.parseInt(s);
        }

        public Place build() {
            return new Place(hierarchy, description, radLat, radLon, neg, codeStandard);
        }
    }
}
