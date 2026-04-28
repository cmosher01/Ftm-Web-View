package nu.mine.mosher.genealogy.xy;

import nu.mine.mosher.genealogy.MathUtils;
import nu.mine.mosher.genealogy.xy.metrics.*;
import nu.mine.mosher.genealogy.xy.shape.*;

import java.util.*;

/**
 * After constructing, must call setHusb(), setWife(), and addChild() (as necessary),
 * and then call calc(), and finally saveSvg()
 */
public class Fami {
    private static final double MIN_DISTANCE = 1.51D;

    private final FontBasedMetrics metricsFont;
    private final ChartMetrics metricsChart;

    private Indi husb;
    private Indi wife;
    private final List<Indi> rChild = new ArrayList<>();

    private Line parentBar1;
    private Line parentBar2;

    private Line descentBarParent;
    private Line descentBarMiddle;
    private Line descentBarChilds;

    private Line childsBar;
    private Line[] rChildBar;



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
    descentBarParentStart |       parentBar2              ^
                          |
                          | < descentBarParent
                          |
                          |               v descentBarMiddle v
    -descentBarMiddleY->  *---------------------------------------------*
                                                                        |
                                                                        | < descentBarChilds
                                                                        |
                                                  descentBarChildsStart |       childsBar          v
                                                      +-----------+-----*-----------------+     <------
                                           rChildBar: |[0]        |[1]                 [2]|     childBarHeight
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

*/

    public void calc() {
        final var couple = new Couple(this.husb, this.wife);
        if (couple.exists()) {
            buildParentBars(couple);
            // ^^^ sets: this.parentBar1, this.parentBar2
        }

        if (!this.rChild.isEmpty()) {
            buildChildsBar(this.rChild);
            // ^^^ sets: this.childsBar
            buildChildBars(this.rChild, this.childsBar);
            // ^^^sets this.rChildBar

            if (couple.exists) {
                final Point2D descentBarParentStart = calcDescentBarParentStart(couple, this.childsBar);
                final Point2D descentBarChildsStart = calcDescentBarChildsStart(descentBarParentStart, this.childsBar);
                final double descentBarMiddleY = calcDescentBarMiddleY(descentBarChildsStart, this.rChild.size());

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





    private void buildParentBars(Couple couple) {
        this.parentBar1 = new Line();
        this.parentBar1.setStart(couple.pt1().translate(0D, -this.metricsFont.getMarriageBarHalfHeight()));
        this.parentBar1.setEnd(couple.pt2().translate(0D, -this.metricsFont.getMarriageBarHalfHeight()));

        this.parentBar2 = new Line();
        this.parentBar2.setStart(couple.pt1().translate(0D, +this.metricsFont.getMarriageBarHalfHeight()));
        this.parentBar2.setEnd(couple.pt2().translate(0D, +this.metricsFont.getMarriageBarHalfHeight()));
    }

    private void buildChildsBar(final List<Indi> rChild) {
        // build childsBar (horizontal)

        // x: left-most child to right-most child
        this.childsBar = new Line();
        this.childsBar.setStartX(rChild.stream().mapToDouble(Indi::x).min().orElseThrow());
        this.childsBar.setEndX(rChild.stream().mapToDouble(Indi::x).max().orElseThrow());

        // y: above the top-most child
        final double topChildPlaque = rChild.stream().map(Indi::getBounds).mapToDouble(Bounds::getMinY).min().orElseThrow();
        this.childsBar.setY(topChildPlaque - this.metricsFont.getChildBarHeight());
    }

    private void buildChildBars(final List<Indi> rChild, final Line childsBar) {
        // build childBars (vertical, one per child)
        this.rChildBar = new Line[rChild.size()];
        for (int i = 0; i < rChild.size(); i++) {
            final var c = rChild.get(i);

            this.rChildBar[i] = new Line();
            this.rChildBar[i].setStartX(c.x());
            this.rChildBar[i].setStartY(childsBar.getStartY());
            this.rChildBar[i].setEnd(c.center());
        }
    }

    /**
     * Calculate the point on the (bottom) marriage bar where the descentBarParent starts.
     * For short bars, use the midpoint.
     * For longer bars (where the two parents are for from each other), use a point close to
     * one of them (the one that is nearest to the children).
     *
     * @param couple the center points of the two parents
     * @param childsBar the horizontal childsBar
     * @return descentBarParent starting point
     */
    private Point2D calcDescentBarParentStart(final Couple couple, final Line childsBar) {
        final var child = childsBar.midpoint();

        // Figure out which parent is closest to the child bar midpoint
        // (and calculate the parent bar start point, near that parent)
        return child.distance(couple.pt1()) < child.distance(couple.pt2()) ?
            calcDescentBarParentStart(couple.pt1(), couple.pt2()) :
            calcDescentBarParentStart(couple.pt2(), couple.pt1());
    }

    /**
     *
     * @param ptNear center point of nearest parent
     * @param ptFar center point of furthest parent
     * @return descentBarParent starting point
     */
    private Point2D calcDescentBarParentStart(final Point2D ptNear, final Point2D ptFar) {
        // Calculate ratio r of distance d along the length of the marriage bar,
        // to the full length, from the near
        // parent towards the far parent, at which the descent line will start.
        // But, if the marriage bar is "short enough", then center the descent bar along it (50%).
        // TODO I think this causes problems if one of the parents is missing:
        // the bar is short enough to qualify for the 50% rule, but that (always?)
        // (sometimes?) causes the descent line start point to be behind the plaque.

        final var ptStart = ptNear.translate(0D, this.metricsFont.getMarriageBarHalfHeight());
        final var ptEnd = ptFar.translate(0D, this.metricsFont.getMarriageBarHalfHeight());
        final double D = Math.max(ptStart.distance(ptEnd), MIN_DISTANCE);

        final double d = this.metricsFont.maxWidthPlaque();

        double r;
        if (D < d * 4.0D) {
            r = 0.5D;
        } else {
            r = d/D;
        }

        return new Point2D(
                (1-r) * ptStart.x() + r * ptEnd.x(),
                (1-r) * ptStart.y() + r * ptEnd.y());
    }

    private Point2D calcDescentBarChildsStart(final Point2D descentBarParentStart, final Line childsBar) {
        // Minimum horiz distance of descentBarChildsStart from ends of childsBar allowed.
        // A visual nicety, it looks bad if the descent line is really close to an end of the childs bar.
        /*
            Not this:
              ===
               |
               |
               |
              +*----------+
              |           |
            +-|-+       +-|-+
            | * |       | * |
            +---+       +---+

            but this instead:
              ===
               |
               |
               +---+
                   |
              +----*------+
              |           |
            +-|-+       +-|-+
            | * |       | * |
            +---+       +---+
         */

        final double minXend = this.metricsChart.medianMinimumDistanceToNeighborScaled() / 4.0D; // TODO move this to ChartMetrics?
        final double minXLen = minXend * 2.0D;

        final double x;
        if (childsBar.length() < minXLen) {
            // corner case: the bar is too short to avoid the case of "too close to the end"
            x = childsBar.midpoint().x();
        } else {
            x = MathUtils.clamp(childsBar.getStartX()+minXend, descentBarParentStart.x(), childsBar.getEndX()-minXend);
        }
        return new Point2D(x, childsBar.getStartY());
    }

    private double calcDescentBarMiddleY(final Point2D descentBarChildsStart, final int nChild) {
        // calculate height of middle descent bar (which is hidden if there's only one child)
        double descentBarMiddleY = descentBarChildsStart.y();
        if (1 < nChild) {
            descentBarMiddleY -= this.metricsFont.getChildBarHeight() / 2.0D;
        }
        ;
        return descentBarMiddleY;
    }

    private void buildDescentBars(final Point2D descentBarParentStart, final double descentBarMiddleY, final Point2D descentBarChildsStart) {
        this.descentBarParent = new Line();
        this.descentBarParent.setStart(descentBarParentStart);
        this.descentBarParent.setEndX(this.descentBarParent.getStartX());
        this.descentBarParent.setEndY(descentBarMiddleY);

        this.descentBarChilds = new Line();
        this.descentBarChilds.setStart(descentBarChildsStart);
        this.descentBarChilds.setEndX(this.descentBarChilds.getStartX());
        this.descentBarChilds.setEndY(descentBarMiddleY);

        this.descentBarMiddle = new Line();
        this.descentBarMiddle.setStart(this.descentBarChilds.getEnd());
        this.descentBarMiddle.setEnd(this.descentBarParent.getEnd());
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
        private final boolean exists;
        private final Point2D pt1;
        private final Point2D pt2;

        public Couple(final Indi husb, final Indi wife) {
            this.exists = !(husb == null && wife == null);

            if (!this.exists) {
                // these points should never be used anywhere
                this.pt1 = Point2D.ZERO;
                this.pt2 = Point2D.ZERO;
            } else if (husb == null) {
                // husband missing; place phantom to left of wife
                this.pt2 = wife.center();
                this.pt1 = this.pt2.translate(-Fami.this.metricsFont.maxWidthPlaque(), 0D);
            } else if (wife == null) {
                // wife missing; place phantom to right of husband
                this.pt1 = husb.center();
                this.pt2 = this.pt1.translate(+Fami.this.metricsFont.maxWidthPlaque(), 0D);
            } else {
                // nominal case
                // husband and wife both present (in either order left/right); no phantom
                this.pt1 = husb.center();
                this.pt2 = wife.center();
            }
        }

        public boolean exists() {
            return this.exists;
        }

        public Point2D pt1() {
            return this.pt1;
        }

        public Point2D pt2() {
            return this.pt2;
        }
    }
}
