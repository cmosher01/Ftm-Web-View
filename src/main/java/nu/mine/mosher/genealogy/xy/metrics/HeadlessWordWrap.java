package nu.mine.mosher.genealogy.xy.metrics;

import java.awt.font.*;
import java.awt.image.BufferedImage;
import java.text.AttributedString;
import java.util.*;

/**
 * Do our own word wrapping, rather than relying on SVG, because (as of 2026), although browsers' svg rendering
 * does word wrap, it doesn't handle the case of splitting a SINGLE WORD that's too long to fit on one line.
 * AWT's LineBreakMeasurer does handle this case correctly.
 */
public final class HeadlessWordWrap {
    private final List<Integer> endings = new ArrayList<>();
    private final double width;
    private final double height;
    private final List<Instrumentation> instrumentation = new ArrayList<>();

    public HeadlessWordWrap(final AttributedString s, final double maxWidth) {
        final var i = s.getIterator();
        final var posBegin = i.getBeginIndex();
        final var posEnd = i.getEndIndex();
        final var m = new LineBreakMeasurer(i, getFontRenderContext());

        double x = 0.0D;
        double y = 0.0D;

        m.setPosition(posBegin);
        while (m.getPosition() < posEnd) {
            final var line = m.nextLayout((float)maxWidth);
            this.endings.add(m.getPosition());
            x = Math.max(x, line.getVisibleAdvance());
            y += line.getAscent();
            addLineToInstrumentation(line, y);
            y += line.getDescent()+line.getLeading();
        }

        this.width = x;
        this.height = y;
    }

    private void addLineToInstrumentation(final TextLayout line, final double y) {
        this.instrumentation.add(new Instrumentation(line, y));
    }

    public double width() {
        return this.width;
    }

    public double height() {
        return this.height;
    }

    public List<Integer> endings() {
        return List.copyOf(this.endings);
    }

    public int nLines() {
        return this.endings.size();
    }

    public List<Instrumentation> instrumentation() {
        return List.copyOf(this.instrumentation);
    }


    private static FontRenderContext getFontRenderContext() {
        final var img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        final var graphics = img.createGraphics();
        return graphics.getFontRenderContext();
    }



    public static class Instrumentation {
        public final double advanceVisible;
        public final double ascent;
        public final double descent;
        public final double leading;
        public final double baselineY;

        private Instrumentation(final TextLayout line, final double y) {
            this.advanceVisible = line.getVisibleAdvance();
            this.ascent = line.getAscent();
            this.descent = line.getDescent();
            this.leading = line.getLeading();
            this.baselineY = y;
        }
    }
}
