package nu.mine.mosher.genealogy.xy;

import nu.mine.mosher.genealogy.xy.metrics.*;
import nu.mine.mosher.genealogy.xy.shape.*;

import java.util.*;

/**
 * After constructing, must call setHusb(), setWife(), and addChild() (as necessary),
 * and then call calc(), and finally saveSvg()
 */
public class Fami {
    private static final double MIN_DISTANCE = 1.51D; // TODO move to metrics?

    private final FontBasedMetrics metricsFont;
    private final ChartMetrics metricsChart;

    // TODO should we move parentbars (and associated calculation methods) to Couple class?
    private Indi husb;
    private Indi wife;
    private Line parentBar1;
    private Line parentBar2;

    // TODO make a DescentBar class?
    private Line.Vert descentBarParent;
    private Line.Horz descentBarMiddle;
    private Line.Vert descentBarChilds;

    // TODO make a Childs, or Children, or Brood class?
    private final List<Indi> rChild = new ArrayList<>();
    private Line.Horz childsBar;
    private Line.Vert[] rChildBar;



    public Fami(final FontBasedMetrics metricsFont, final ChartMetrics metricsChart) {
        this.metricsFont = metricsFont;
        this.metricsChart = metricsChart;
    }



    public void setHusb(final Indi indi) {
        this.husb = indi; // could be null
    }

    public void setWife(final Indi indi) {
        this.wife = indi; // could be null
    }

    public void addChild(final Indi indi) {
        if (Objects.nonNull(indi)) {
            this.rChild.add(indi);
        }
    }


/*

                                  parentBar1              v
                    -----------------------------       <------
                  hu*sb                       wi*fe     metrics.marriageBarHalfHeight * 2
                    ------*----------------------       <------
     descentBarParentTop* |       parentBar2              ^
                          |
                          | < descentBarParent
                          |
                          |               v descentBarMiddle v
   -descentBarMiddleY-->  *---------------------------------------------*
                                                                        |
                                                                        | < descentBarChilds
                                                                        |
                                                descentBarChildsBottom* |       childsBar          v
                                                      +-----------+-----*-----------------+     <------
                                           rChildBar: |[0]        |[1]                 [2]|     childBarHeight, above rChildBar[1]
                       ----minimum-Y-of-children--->  |         +-|-+                     |     <------
                                                      |         |c*2|                   +-|-+     ^
                                                    +-|-+       +---+                   |c*3|
                                                    |c*1|                               +---+
                                                    +---+



    The (x,y) coordinate associated with each person represents the center point of their plaque.
    All calculations of chart layout are based on this set of points, as well as some font-based metrics.
    An asterisk (*) on this diagram represents a point (midpoint of plaque, or endpoints of descent bars)
    Layout lines (marriage bars, descent bars) are actually drawn all the way to these center points,
    although they are hidden behind the opaque plaques.
    "Marriage bars" (parentBar1 and 2) go between each parent's midpoint (X) and (Y +/- half barHeight).
    Descent bar has 3 segments: Parent, Middle, Childs (each name is 6 letters, for pretty source code).
    Descent bar goes between descentBarParentStart and descentBarChildStart.
    For single-child broods, descentBarMiddle and descentBarChilds are not visible.
    "rChildBar" is array of each child's vertical line from childBar down to (center of) child's plaque.
    "childHeight" is distance from the highest child's top (child c2 in this diagram), up to the childBar.

    The childBars, descentBarParent, and descentBarChilds are always vertical.
    The childsBar, and descentBarMiddle are always horizontal.
    The marriage bar could be at any angle.
*/

    public void calc() {
        final var couple = new Couple(this.husb, this.wife);
        this.parentBar1 = couple.bar1();
        this.parentBar2 = couple.bar2();

        if (!this.rChild.isEmpty()) {
            this.childsBar = buildChildsBar(this.rChild);
            this.rChildBar = buildChildBars(this.rChild, this.childsBar);

            if (couple.exists()) {
                final Point descentBarParentStart = calcDescentBarParentStart(couple, this.childsBar.midpoint());
                final Point descentBarChildsStart = calcDescentBarChildsStart(descentBarParentStart.x(), this.childsBar);
                final double descentBarMiddleY = calcDescentBarMiddleY(descentBarChildsStart.y(), this.rChild.size());

                buildDescentBars(descentBarParentStart, descentBarMiddleY, descentBarChildsStart);
                // ^^^ sets: this.descentBarParent, this.descentBarMiddle, this.descentBarChilds
            }
        }
    }









    public void saveSvg(final SvgBuilder svg) {
        svg.addLine(this.parentBar1);
        svg.addLine(this.parentBar2);
        svg.addLine(this.descentBarParent);
        svg.addLine(this.descentBarMiddle);
        svg.addLine(this.descentBarChilds);
        svg.addLine(this.childsBar);
        if (!this.rChild.isEmpty()) {
            Arrays.asList(this.rChildBar).forEach(svg::addLine);
        }
    }








    // build childsBar (horizontal)
    // empty list will throw exception
    private Line.Horz buildChildsBar(final List<Indi> rChild) {
        // get top y, and min/max x values of child plaques
        final var yTop = rChild.stream().map(Indi::getBounds).mapToDouble(Bounds::top).min().getAsDouble();
        final var xMin = rChild.stream().mapToDouble(Indi::x).min().getAsDouble();
        final var xMax = rChild.stream().mapToDouble(Indi::x).max().getAsDouble();
        return Line.Horz.create(yTop, xMin, xMax).translate(-this.metricsFont.getChildBarHeight());
    }

    // build childBars (vertical, one per child)
    private Line.Vert[] buildChildBars(final List<Indi> rChild, final Line.Horz childsBar) {
        final var rChildBar = new Line.Vert[rChild.size()];
        for (int i = 0; i < rChild.size(); i++) {
            final var child = rChild.get(i);
            rChildBar[i] = Line.Vert.create(child.x(), childsBar.y(), child.y());
        }
        return rChildBar;
    }

    /**
     * Calculate the point on the (bottom) marriage bar where the descentBarParent starts.
     * For short bars, use the midpoint.
     * For longer bars (where the two parents are for from each other), use a point close to
     * one of them (the one that is nearest to the children).
     *
     * @param couple the center points of the two parents
     * @param child the midpoint of the horizontal childsBar
     * @return descentBarParent starting point
     */
    private Point calcDescentBarParentStart(final Couple couple, final Point child) {
        // Figure out which parent is closest to the child bar midpoint
        // (and calculate the parent bar start point, near that parent)
        // note this is the ONLY place couple's pt1 and pt2 methods are used
        final Point ptNear; //ptNear center point of nearest parent
        final Point ptFar;  //ptFar center point of furthest parent
        if (child.distance(couple.pt1()) < child.distance(couple.pt2())) {
            ptNear = couple.pt1();
            ptFar = couple.pt2();
        } else {
            ptNear = couple.pt2();
            ptFar = couple.pt1();
        }

        // Calculate ratio r of distance d along the length of the marriage bar,
        // to the full length, from the near
        // parent towards the far parent, at which the descent line will start.
        // But, if the marriage bar is "short enough", then center the descent bar along it (50%).
        // TODO I think this causes problems if one of the parents is missing:
        // the bar is short enough to qualify for the 50% rule, but that (always?)
        // (sometimes?) causes the descent line start point to be behind the plaque.

        // TODO this calculation of the lower marriage bar is redundant. Can we get the
        // marriage bar from the Couple instead? Do we need to worry about which parent is
        // visually on the left? Or which end of the line is the "start" and which is the "end".
        // It's confusing, and needs to be clarified and simplified.

        // We should be able to tell Couple the point (on the childsBar), and it should figure
        // out which parent is closest, and using the bottom parent bar, figure out the
        // descent bar start point.

        final var bar = Line.create(ptNear, ptFar)
            .translate(0D, this.metricsFont.getMarriageBarHalfHeight());

        // total distance (use min to prevent division by zero later)
        final double D = Math.max(bar.length(), MIN_DISTANCE);

        // distance along line from starting point
        final double d = this.metricsFont.maxWidthPlaque();

        final double r; // ratio d to D
        if (D < d * 4.0D) { // short marriage bar
            r = 0.5D; // center between parents
        } else {
            r = d/D;
        }

        return bar.section(r);
    }

    /*
        Ideally we'd like to simply start at the childsBar midpoint, but this doesn't
        always look visually appealing. We move the starting point towards the parent's
        bar starting point, but don't get too close to the ends of the childsBar.
     */
    private Point calcDescentBarChildsStart(final double descentBarParentX, final Line.Horz childsBar) {
        // Minimum horiz distance of descentBarChildsStart from ends of childsBar allowed.
        // A visual nicety, it looks bad if the descent line is really close to an end of the childs bar.
        /*
            Not this:
              ===
               |
               |
               |
              +*--------------+
              |               |
            +-|-+           +-|-+
            | * |           | * |
            +---+           +---+

            but this instead:
              ===
               |
               |
               +---+
                   |
              +----*----------+
              |               |
            +-|-+           +-|-+
            | * |           | * |
            +---+           +---+




   minXend--->|   |<-             ->|   |<---minXend

              +---[-----------------]---+   <---[allowable range for start point]
              |                         |
            +-|-+                     +-|-+
            | * |                     | * |
            +---+                     +---+

            (but if bar is shorter than 2 * minXLen, just use the midpoint)

         */

        final double minXend = this.metricsChart.medianMinimumDistanceToNeighborScaled() / 4.0D; // TODO move this to ChartMetrics?

        final double x;
        if (childsBar.length() < minXend * 2.0D) {
            // corner case: the bar is too short to avoid the case of "too close to the end"
            x = childsBar.midpoint().x();
        } else {
            // clamp the X of the descent bar to between (left end + offset) and (right end - offset)
            x = Math.clamp(descentBarParentX, childsBar.p1().x()+minXend, childsBar.p2().x()-minXend);
        }
        return Point.create(x, childsBar.y());
    }

    private double calcDescentBarMiddleY(final double descentBarChildsY, final int nChild) {
        // calculate height of middle descent bar (which is hidden if there's only one child)
        double descentBarMiddleY = descentBarChildsY;
        if (1 < nChild) {
            descentBarMiddleY -= this.metricsFont.getChildBarHeight() / 2.0D;
        }
        return descentBarMiddleY;
    }

    private void buildDescentBars(final Point descentBarParentTop, final double descentBarMiddleY, final Point descentBarChildsBtm) {
        this.descentBarParent = Line.Vert.create(descentBarParentTop.x(), descentBarParentTop.y(), descentBarMiddleY);
        this.descentBarChilds = Line.Vert.create(descentBarChildsBtm.x(), descentBarChildsBtm.y(), descentBarMiddleY);
        this.descentBarMiddle = Line.Horz.create(descentBarMiddleY, this.descentBarParent.x(), this.descentBarChilds.x());
    }


    /**
     * <p>
     * Represents two parents' center points.
     * The main purpose of this class is to help handle
     * the case where exactly one parent exists.
     * In this case, it calculates the where the other
     * parent (the "phantom") would be placed.
     * </p>
     *
     * TODO Can we display a "?" plaque for a phantom?
     */
    private class Couple {
        private final double BARLINE_DELTA_Y = Fami.this.metricsFont.getMarriageBarHalfHeight();
        private final double PHANTOM_DELTA_X = Fami.this.metricsFont.maxWidthPlaque();
        private final boolean exists;
        private final Point ptHusb;
        private final Point ptWife;

        public Couple(final Indi husb, final Indi wife) {
            this.exists = !(husb == null && wife == null);

            if (!this.exists) {
                // these points should never be used anywhere
                this.ptHusb = Point.ZERO;
                this.ptWife = Point.ZERO;
            } else if (husb == null) {
                // husband missing; place phantom to left of wife
                this.ptWife = wife.center();
                this.ptHusb = this.ptWife.translate(-PHANTOM_DELTA_X, 0D);
            } else if (wife == null) {
                // wife missing; place phantom to right of husband
                this.ptHusb = husb.center();
                this.ptWife = this.ptHusb.translate(+PHANTOM_DELTA_X, 0D);
            } else {
                // nominal case
                // husband and wife both present (in either order left/right); no phantom
                this.ptHusb = husb.center();
                this.ptWife = wife.center();
            }
        }

        public boolean exists() {
            return this.exists;
        }

        public Point pt1() {
            return this.ptHusb;
        }

        public Point pt2() {
            return this.ptWife;
        }

        public Line bar1() {
            return bar(-BARLINE_DELTA_Y);
        }

        public Line bar2() {
            return bar(+BARLINE_DELTA_Y);
        }

        private Line bar(final double dy) {
            if (!this.exists) {
                return null;
            }
           return Line.create(this.ptHusb, this.ptWife).translate(0D, dy);
        }
    }
}
