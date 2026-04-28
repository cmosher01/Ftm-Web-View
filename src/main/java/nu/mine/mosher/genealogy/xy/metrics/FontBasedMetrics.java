package nu.mine.mosher.genealogy.xy.metrics;

import nu.mine.mosher.genealogy.xy.shape.Insets;
import org.slf4j.*;

import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.util.Map;

public final class FontBasedMetrics {
    private static final Logger LOG =  LoggerFactory.getLogger(FontBasedMetrics.class);



    private static final String FONT_FAMILY_NAME = "Noto Sans";

    private static final double FONT_SIZE_POINTS_NOMINAL = 8.0D;
    private static final double LINE_HEIGHT_POINTS = 11.0D;
    private static final double FONT_SIZE_POINTS_NOMINAL_SMALL = 6.0D;
    private static final double LINE_HEIGHT_POINTS_SMALL = 8.0D;

    private static final String PLAQUE_MAX = "MMMMMMMMMMMMMMMM"; // TODO is 16 M's wide enough?



    /**
     * This is the maximum with of TEXT in a person's rectangle.
     * Around this text area there will be some PADDING,
     * and then the RECTANGLE itself around that.
     */
    private final double maxWidthPlaque;
    private final double heightLine;
    private final double ascent;


    /**
     * Initializes the font, and calculates the maximum width of text
     * within a person's rectangle (which should be used for word-wrapping
     * all text within the rectangle).
     */
    public FontBasedMetrics() {
        // Calculate maximum width for person text as width of 16 bold capital Ms.
        final var bold = Map.of(
                TextAttribute.FAMILY, FONT_FAMILY_NAME,
                TextAttribute.SIZE, FONT_SIZE_POINTS_NOMINAL,
                TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        final var Ms = new AttributedString(PLAQUE_MAX, bold);
        final var w = new HeadlessWordWrap(Ms, Double.POSITIVE_INFINITY);
        this.maxWidthPlaque = w.width();
        this.heightLine = w.height();
        this.ascent = w.instrumentation().getFirst().ascent;
        LOG.info("max font-based metrics: line-width: {}, line-height: {}, ascent: {}", this.maxWidthPlaque, this.heightLine, this.ascent);
    }

    public String fontFamily() {
        return FONT_FAMILY_NAME;
    }

    public double fontSize() {
        return FONT_SIZE_POINTS_NOMINAL;
    }

    public double fontSizeSmall() {
        return FONT_SIZE_POINTS_NOMINAL_SMALL;
    }

    public double lineHeight() {
        return LINE_HEIGHT_POINTS;
    }

    public double lineHeightSmall() {
        return LINE_HEIGHT_POINTS_SMALL;
    }

    public double fontAscent() {
        return this.ascent;
    }

    public double maxWidthPlaque() {
        return this.maxWidthPlaque;
    }

    public Insets paddingChart() {
        return new Insets(100.0D);
    }

    public Insets paddingPlaque() {
        return new Insets(this.heightLine);
    }

    public double getMarriageBarHalfHeight() {
        return FONT_SIZE_POINTS_NOMINAL / 4.0D;
    }

    public double getChildBarHeight() {
        return FONT_SIZE_POINTS_NOMINAL * 4.0D;
    }
}
