package nu.mine.mosher.gedcom;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.*;
import java.util.stream.Collectors;

import static nu.mine.mosher.gedcom.StringUtils.safe;
import static nu.mine.mosher.gedcom.XmlUtils.e;


@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Place {
    private static final Logger LOG =  LoggerFactory.getLogger(Place.class);

    private final List<String> hierarchy;
    private List<String> display;
    private String sDisplay = "";
    private final String description;
    private final Optional<GeoCoords> coords;
    private final boolean neg; // TODO what is this flag for?
    private final int codeStandard; // TODO what place-coding standard is this?
    private boolean ditto;

    private Place(List<String> hierarchy, String description, Optional<GeoCoords> coords, boolean neg, int codeStandard) {
        this.hierarchy = List.copyOf(hierarchy);
        this.display = new ArrayList<>(hierarchy);
        this.description = description;
        this.coords = coords;
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

    public boolean isDitto() {
        return this.ditto;
    }

    public boolean isBlank() {
        return !this.ditto && this.sDisplay.isBlank() && safe(this.description).isBlank();
    }

    public void appendTo(final Element parent) {
        if (this.ditto) {
            parent.setTextContent("\u00A0\u201D");
        } else {
            final Element name = e(parent, "span");
            name.setTextContent(this.sDisplay.isBlank() ? this.description : this.sDisplay);

            if (this.coords.isPresent()) {
                final Element sup = e(parent, "sup");
                final Element google = e(sup, "a");
                google.setAttribute("href", this.coords.get().urlGoogleMaps().toExternalForm());
                google.setTextContent(new String(Character.toChars(0x1F5FA)));
            }
        }
    }

    public void setDisplay(final List<String> display) {
        this.display = new ArrayList<>(display);
        this.sDisplay = String.join(", ", this.display);
    }

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof Place that)) {
            return false;
        }
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
        private Optional<GeoCoords> coords;
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
            this.coords = GeoCoords.parse(s[s.length-2], s[s.length-1]);

            this.codeStandard = parseCode(s[s.length-3]);

            this.neg = false;
            if (this.codeStandard < 0) {
                this.neg = true;
                this.codeStandard = -this.codeStandard;
            }
        }

        private static int parseCode(final String s) {
            if (s.isBlank()) {
                return 0;
            }
            return Integer.parseInt(s);
        }

        public Place build() {
            return new Place(hierarchy, description, coords, neg, codeStandard);
        }
    }
}
