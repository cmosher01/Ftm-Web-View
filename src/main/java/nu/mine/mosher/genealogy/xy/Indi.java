package nu.mine.mosher.genealogy.xy;

import nu.mine.mosher.genealogy.IndexedPerson;
import nu.mine.mosher.genealogy.xy.metrics.*;
import nu.mine.mosher.genealogy.xy.shape.*;
import org.slf4j.*;

import java.awt.font.TextAttribute;
import java.sql.SQLException;
import java.text.AttributedString;
import java.util.*;
import java.util.regex.*;

import static java.util.Optional.empty;



public class Indi {
    private static final Logger LOG =  LoggerFactory.getLogger(Indi.class);

    private final int id; // FTM database primary key column
    private final UUID refn; // _ID fact (uuid)
    private final Point2D xy;
    private final String nameGiven;
    private final String nameSur;
    private final HeadlessWordWrap wrapNameFull;
    private final String lifespan;
    private final HeadlessWordWrap wrapLifespan;
    private final String tagline;
    private final HeadlessWordWrap wrapTagline;
    private final Bounds bounds;
    private final boolean isRecent;





    public static Indi buildFromIndexedPerson(final IndexedPerson p, final FontBasedMetrics metrics, final double scaleFactor) {
        final var sBirth = p.dateBirth().simplisticNoFiller();
        final var sDeath = p.dateDeath().simplisticNoFiller();
        final var lifespan = "("+sBirth+"\u2013"+sDeath+")";

        final var tagline =
            (Objects.nonNull(p.birthplace()) && !p.birthplace().isBlank())
            ? p.birthplace().description()
            :
            (Objects.nonNull(p.anyplace()) && !p.anyplace().isBlank())
            ? p.anyplace().description()
            :
            "";

        return new Indi(p.xy(), p.pkid(), p.preferRefn(), p.gedcomname(), lifespan, tagline, p.isRecent(), metrics, scaleFactor);
    }

    private Indi(final String xy, final int id, final UUID refn, final String name, final String lifespan, final String tagline, final boolean isRecent, final FontBasedMetrics metrics, double scaleFactor) {
        final String n = Optional.ofNullable(name).orElse("").strip();

        final var tXY = alwaysCoord(xy).multiply(scaleFactor);
        final var tNameGiven = parseNameGiven(n);
        final var tNameSur = parseNameSur(n);
        final var tFullName = buildUnattributedFullName(tNameGiven, tNameSur);
        final var tTagline = Optional.ofNullable(tagline).orElse("").strip();



        final var attrsFont = Map.of(
            TextAttribute.FAMILY, metrics.fontFamily(),
            TextAttribute.KERNING, TextAttribute.KERNING_ON,
            TextAttribute.SIZE, metrics.fontSize());
        final var atrsFullName = new AttributedString(tFullName, attrsFont);
        // set given name to bold
        atrsFullName.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD, 0, tNameGiven.length());
        this.wrapNameFull = new HeadlessWordWrap(atrsFullName, metrics.maxWidthPlaque());

        final var attrsFontSmall = Map.of(
            TextAttribute.FAMILY, metrics.fontFamily(),
            TextAttribute.KERNING, TextAttribute.KERNING_ON,
            TextAttribute.SIZE, metrics.fontSizeSmall());
        final var atrsLifespan = new AttributedString(lifespan, attrsFontSmall);
        this.wrapLifespan = new HeadlessWordWrap(atrsLifespan, metrics.maxWidthPlaque());

        if (tTagline.isBlank()) {
            this.wrapTagline = null;
        } else {
            final var atrsTagline = new AttributedString(tTagline, attrsFontSmall);
            this.wrapTagline = new HeadlessWordWrap(atrsTagline, metrics.maxWidthPlaque());
//            LOG.info("-------------------------------------------");
//            LOG.info("w={} n={} h={} \"{}\"", this.wrapTagline.width(), this.wrapTagline.endings().size(), this.wrapTagline.height(), tTagline);
//            LOG.info("-------------------------------------------");
        }

        final double width = Math.max(Math.max(this.wrapNameFull.width(), this.wrapLifespan.width()), (Objects.isNull(this.wrapTagline) ? 0 : this.wrapTagline.width()));

        final double height =
            metrics.lineHeight() * (this.wrapNameFull.nLines() + this.wrapLifespan.nLines()) +
            metrics.lineHeightSmall() * (Objects.isNull(this.wrapTagline) ? 0 : this.wrapTagline.nLines());

        this.bounds = new Bounds(
            (tXY.getX()-width/2D),
            ((tXY.getY()-height/2D)+metrics.fontAscent()),
            width,
            height);

        this.id = id;
        this.refn = refn;
        this.xy = tXY;
        this.nameGiven = tNameGiven;
        this.nameSur = tNameSur;
        this.lifespan = lifespan;
        this.tagline = tTagline;
        this.isRecent = isRecent;
    }

    private static String buildUnattributedFullName(final String nameGiven, final String nameSur) {
        final var s = new StringBuilder(nameGiven.length()+1+nameSur.length());
        s.append(nameGiven);
        if (!nameSur.isBlank()) {
            s.append(" ");
            s.append(nameSur);
        }
        return s.toString();
    }




    public Point2D center() {
        return this.xy;
    }

    public double x() {
        return this.xy.getX();
    }

    public double y() {
        return this.xy.getY();
    }

    public int getId() {
        return id;
    }

    public UUID getRefn() {
        return this.refn;
    }

    public String getNameGiven() {
        return nameGiven;
    }

    public String getNameSur() {
        return nameSur;
    }

    public HeadlessWordWrap getWrapNameFull() {
        return this.wrapNameFull;
    }

    public HeadlessWordWrap getWrapLifespan() {
        return this.wrapLifespan;
    }

    public HeadlessWordWrap getWrapTagline() {
        return this.wrapTagline;
    }

    public String getLifespan() {
        return lifespan;
    }

    public String getTagline() {
        return tagline;
    }

    public Bounds getBounds() {
        return bounds;
    }

    public boolean isRecent() {
        return this.isRecent;
    }

    public void saveSvg(final SvgBuilder svg) throws SQLException {
        svg.addPerson(this);
    }





    private static final Pattern PAT_NAME = Pattern.compile("(.*)/([^/]*?)/([^/]*?)");

    private static String parseNameSur(final String name) {
        final Matcher matcher = PAT_NAME.matcher(name);
        if (!matcher.matches()) {
            return "";
        }

        return matcher.group(2).strip();
    }

    private static String parseNameGiven(final String name) {
        var ret = parseNameGivenRaw(name);
        if (ret.isBlank()) {
            ret = "?";
        }
        return ret;
    }

    private static String parseNameGivenRaw(final String name) {
        final Matcher matcher = PAT_NAME.matcher(name);
        if (!matcher.matches()) {
            return name.strip();
        }
        final String n1 = matcher.group(1);
        final String n2 = matcher.group(3);
        if (n1.isBlank() && n2.isBlank()) {
            return "";
        }
        if (!n1.isBlank() && n2.isBlank()) {
            return n1.strip();
        }
        if (n1.isBlank() && !n2.isBlank()) {
            return n2.strip();
        }
        return n1.strip()+" ~ "+n2.strip();
    }

    public static Point2D alwaysCoord(final String xy) {
        final var opt = toCoord(xy);
        return opt.orElse(Point2D.ZERO);
    }

    private static Optional<Point2D> toCoord(final String xy) {
        if (Objects.isNull(xy) || xy.isEmpty()) {
            return empty();
        }

        final String[] fields = xy.split("\\s+");
        if (fields.length < 2) {
            return empty();
        }

        final Optional<Double> x = parseCoord(fields[0]);
        if (x.isEmpty()) {
            return empty();
        }

        final Optional<Double> y = parseCoord(fields[1]);
        if (y.isEmpty()) {
            return empty();
        }

        return Optional.of(new Point2D(x.get(), y.get()));
    }

    private static Optional<Double> parseCoord(final String s) {
        if (Objects.isNull(s) || s.isBlank()) {
            return empty();
        }
        try {
            return Optional.of(Double.parseDouble(s.strip()));
        } catch (final NumberFormatException ignore) {
            return empty();
        }
    }
}
