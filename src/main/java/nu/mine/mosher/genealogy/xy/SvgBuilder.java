package nu.mine.mosher.genealogy.xy;

import nu.mine.mosher.genealogy.*;
import nu.mine.mosher.genealogy.xy.metrics.*;
import nu.mine.mosher.genealogy.xy.shape.*;
import org.slf4j.*;
import org.w3c.dom.*;

import java.awt.font.TextAttribute;
import java.sql.SQLException;
import java.text.AttributedString;
import java.util.*;

public class SvgBuilder {
    private static final Logger LOG =  LoggerFactory.getLogger(SvgBuilder.class);

    private static final String W3C_SVG_NS_URI = "http://www.w3.org/2000/svg";
    private static final String W3C_XLINK_NS_URI = "http://www.w3.org/1999/xlink";

    private final Document doc;
    private final Element svg;
    private final String nameTree;
    private final FontBasedMetrics metrics;
    private final RbacAuthorizer role;


    public SvgBuilder(final RbacAuthorizer role, final Bounds bounds, final Element parent, final String nameTree, final FontBasedMetrics metrics) {
        this.doc = parent.getOwnerDocument();
        this.nameTree = nameTree;
        this.metrics = metrics;
        this.role = role;

        this.svg = this.doc.createElementNS(W3C_SVG_NS_URI, "svg");
        final var viewport = bounds.inset(metrics.paddingChart());
        this.svg.setAttribute("width", px(viewport.getWidth()));
        this.svg.setAttribute("height", px(viewport.getHeight()));
        this.svg.setAttribute("viewBox", viewBox(viewport));
        parent.appendChild(svg);
    }



    public void addLine(final Line line) {
        if (Objects.isNull(line)) {
            return;
        }

        final var e = this.doc.createElementNS(W3C_SVG_NS_URI, "line");

        e.setAttribute("x1", uc(line.getStartX()));
        e.setAttribute("y1", uc(line.getStartY()));

        e.setAttribute("x2", uc(line.getEndX()));
        e.setAttribute("y2", uc(line.getEndY()));

        this.svg.appendChild(e);
    }

    private boolean can(final RbacAuthorizer role, boolean recent) throws SQLException {
        boolean can = false;
        if (role.can(RbacPermission.READ)) {
            can = role.can(recent ? RbacPermission.PRIVATE : RbacPermission.PUBLIC);
        }
        return can;
    }

    public void addPerson(final Indi indi) throws SQLException {
        final var link = this.doc.createElementNS(W3C_SVG_NS_URI, "a");
        link.setAttributeNS(W3C_XLINK_NS_URI, "href", formatLink(indi.getRefn(), this.nameTree));
        this.svg.appendChild(link);

        final var text = this.doc.createElementNS(W3C_SVG_NS_URI, "text");
        {
            final var rect = this.doc.createElementNS(W3C_SVG_NS_URI, "rect");
            setBoundsAttributes(indi, rect, text);
            link.appendChild(rect);
            link.appendChild(text);
        }

        if (!can(this.role, indi.isRecent())) {
            final var redacted = this.doc.createElementNS(W3C_SVG_NS_URI, "tspan");
            redacted.setAttribute("class", "redacted"); // TODO create CSS for "redacted" class elements?
            text.appendChild(redacted);
            addTextLines(wRedacted(this.metrics), "[redacted]", redacted, indi.x(), 0.0D);
        } else {
            final var eNameGiven = this.doc.createElementNS(W3C_SVG_NS_URI, "tspan");
            eNameGiven.setAttribute("class", "nameGiven");
            text.appendChild(eNameGiven);

            // put a space between the given name and the surname (if they both exist)
            if (!indi.getNameGiven().isBlank() && !indi.getNameSur().isBlank()) {
                final var space = this.doc.createElementNS(W3C_SVG_NS_URI, "tspan");
                space.appendChild(this.doc.createTextNode(" "));
                text.appendChild(space);
            }

            final var eNameSur = this.doc.createElementNS(W3C_SVG_NS_URI, "tspan");
            eNameSur.setAttribute("class", "nameSur");
            text.appendChild(eNameSur);

            addTextNameLines(indi.getWrapNameFull(), indi.getNameGiven(), eNameGiven, indi.getNameSur(), eNameSur, indi.x(), metrics.lineHeight());



            final var eDate = this.doc.createElementNS(W3C_SVG_NS_URI, "tspan");
            eDate.setAttribute("class", "date");
            text.appendChild(eDate);
            addTextLines(indi.getWrapLifespan(), indi.getLifespan(), eDate, indi.x(), metrics.lineHeight());

            if (!indi.getTagline().isBlank()) {
                final var eTagLine = this.doc.createElementNS(W3C_SVG_NS_URI, "tspan");
                eTagLine.setAttribute("class", "tagline");
                text.appendChild(eTagLine);
                addTextLines(indi.getWrapTagline(), indi.getTagline(), eTagLine, indi.x(), metrics.lineHeightSmall());
            }
        }
    }

    private void setBoundsAttributes(final Indi indi, final Element rect, final Element text) throws SQLException {
        final Bounds boundsText;

        if (!can(this.role, indi.isRecent())) {
            boundsText = boundsRedacted(this.metrics, indi.center());
        } else {
            boundsText = indi.getBounds();
        }

        text.setAttribute("class", "person");
        text.setAttribute("x", uc(boundsText.getMinX()));
        text.setAttribute("y", uc(boundsText.getMinY()));
        text.setAttribute("data-width", uc(boundsText.getWidth()));
        text.setAttribute("data-height", uc(boundsText.getHeight()));
        text.setAttribute("data-refn", indi.getRefn().toString());
        text.setAttribute("data-pkid", Integer.toString(indi.getId(), 10));
        text.setAttribute("data-mid-x", uc(indi.x()));
        text.setAttribute("data-mid-y", uc(indi.y()));

        // Calculate bounds for rectangle based on bounds of the text.
        // Note: the FONT BASELINE is considered the top bound of the text area.
        final Bounds boundsRect = boundsText.translate(0D, -this.metrics.fontAscent()).inset(metrics.paddingPlaque());

        rect.setAttribute("x", uc(boundsRect.getMinX()));
        rect.setAttribute("y", uc(boundsRect.getMinY()));
        rect.setAttribute("width", uc(boundsRect.getWidth()));
        rect.setAttribute("height", uc(boundsRect.getHeight()));
    }

    private void addTextLines(final HeadlessWordWrap wrap, final String s, final Element e, final double x, final double dy) {
        final var endings = wrap.endings();

        int posBegin = 0;
        for (final var posEnd : endings) {
            final var line = s.substring(posBegin, posEnd).strip();

            final var span = this.doc.createElementNS(W3C_SVG_NS_URI, "tspan");
            span.setAttribute("x", uc(x));
            if (Double.compare(1e-4D, dy) < 0) {
                span.setAttribute("dy", uc(dy));
            }
            span.appendChild(this.doc.createTextNode(line.strip()));
            e.appendChild(span);

            posBegin = posEnd;
        }
    }

    private void addTextNameLines(final HeadlessWordWrap wrap, final String g, final Element eNameGiven, final String s, final Element eNameSur, final double x, final double dy) {
        final var endings = wrap.endings();

        int i = 0;

        // Check if the given name EXACTLY ENDED the physical line (versus
        // if we will need to add some of the surname on the same physical line);
        boolean exactlyEnded = false;

        // TODO add some sanity checks here and below, to prevent infinite loops, (and out of bounds?)
        int gc = 0;
        while (gc < g.length()) {
            final var e = endings.get(i);
            if (e == g.length()+1) {
                exactlyEnded = true;
            }
            final var t = Math.min(e, g.length());
            final var L = g.substring(gc, t);
            final var span = this.doc.createElementNS(W3C_SVG_NS_URI, "tspan");
            span.setAttribute("x", uc(x));
            if (0 < gc) {
                span.setAttribute("dy", uc(dy));
            }
            span.appendChild(this.doc.createTextNode(L.strip()));
            eNameGiven.appendChild(span);
            if (e <= t) {
                ++i;
            }
            gc = t;
        }

        int sc = 0;
        while (sc < s.length()) {
            final var e = endings.get(i);
            final var t = e-g.length()-1;
            final var L = s.substring(sc, t);
            if (!(sc == 0 && L.isBlank())) {
                final var span = this.doc.createElementNS(W3C_SVG_NS_URI, "tspan");
                if (exactlyEnded) {
                    span.setAttribute("x", uc(x));
                    span.setAttribute("dy", uc(dy));
                }
                span.appendChild(this.doc.createTextNode(L.strip()));
                eNameSur.appendChild(span);
            }
            ++i;
            sc = t;
        }
    }





    private static HeadlessWordWrap wRedacted(final FontBasedMetrics metrics) {
        final var atr = Map.of(
                TextAttribute.FAMILY, metrics.fontFamily(),
                TextAttribute.KERNING, TextAttribute.KERNING_ON,
                TextAttribute.SIZE, metrics.fontSize());
        final var atrs = new AttributedString("[redacted]", atr);
        return new HeadlessWordWrap(atrs, metrics.maxWidthPlaque());
    }

    private static Bounds boundsRedacted(final FontBasedMetrics metrics, final Point2D center) {
        final var wrap = wRedacted(metrics);
        final double width = wrap.width();
        final double x = center.x()-width/2.0D;
        final double height = metrics.lineHeight() * wrap.nLines();
        final double y = (center.y()-height/2.0D)+ metrics.fontAscent();
        return new Bounds(x, y, width, height);
    }

    private static String formatLink(final UUID refn, final String treename) {
        return String.format("?tree=%s&person_uuid=%s", treename, refn);
    }

    // double, as svg user coordinates
    private static String uc(final Double f) {
        return String.format("%.2f", f);
    }

    // double, as svg pixels
    private static String px(final Double f) {
        return uc(f)+"px";
    }

    private static String viewBox(final Bounds bounds) {
        return String.format("%.2f %.2f %.2f %.2f", bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
    }
}
