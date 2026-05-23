/*
    Copyright © 2026, Christopher Alan Mosher, New York, New York, USA, <cmosher01@gmail.com>.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
