package nu.mine.mosher.genealogy.xy;

import nu.mine.mosher.genealogy.MathUtils;
import nu.mine.mosher.genealogy.xy.metrics.*;
import nu.mine.mosher.genealogy.xy.shape.*;

import java.util.*;

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
        this.husb = indi;
    }

    public void setWife(final Indi indi) {
        this.wife = indi;
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
        if (this.husb == null && this.wife == null && this.rChild.isEmpty()) {
            return;
        }

        final Couple couple = new Couple(this.husb, this.wife);

        // calculate "===" marriage/parent bar
        if (couple.exists()) {
            buildParentBars(couple);
        }

        if (!this.rChild.isEmpty()) {
            buildChildsBar(this.rChild);
            buildChildBars(this.rChild, this.childsBar);

            // if parent(s) exist, draw descent bars (parent, middle, childs)
            if (couple.exists) {
                final Point2D descentBarParentStart = calcDescentBarParentStart(couple, this.childsBar);
                final Point2D descentBarChildsStart = calcDescentBarChildsStart(descentBarParentStart, this.childsBar);
                double descentBarMiddleY = calcDescentBarMiddleY(descentBarChildsStart, this.rChild.size());

                buildDescentBars(descentBarParentStart, descentBarMiddleY, descentBarChildsStart);
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

    private Point2D calcDescentBarParentStart(final Couple couple, final Line childsBar) {
        // midpoint of child bar
        final var child = new Point2D((childsBar.getStartX() + childsBar.getEndX()) / 2.0D, childsBar.getStartY());

        // Figure out which parent is closest to the child bar midpoint
        // and calculate the parent bar start point, near that parent
        return child.distance(couple.pt1()) < child.distance(couple.pt2()) ?
            calcDescentBarParentStart(couple.pt1(), couple.pt2()) :
            calcDescentBarParentStart(couple.pt2(), couple.pt1());
    }

    // calculate the point on the (bottom) marriage bar where the descentBarParent starts
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
            r = 1.0D/2.0D;
        } else {
            r = d/D;
        }

        return new Point2D(
                (1-r) * ptStart.getX() + r * ptEnd.getX(),
                (1-r) * ptStart.getY() + r * ptEnd.getY());
    }

    private Point2D calcDescentBarChildsStart(final Point2D descentBarParentStart, final Line childsBar) {
        // Minimum horiz distance of descentBarChildsStart from ends of childsBar allowed.
        // A visual nicety, it looks bad if the descent line is really close to an end of the childs bar.
        final double minXend = this.metricsChart.medianMinimumDistanceToNeighborScaled() / 4.0D;
        final double x;
        if (childsBar.getEndX() - childsBar.getStartX() < minXend*2.0D) {
            x = (childsBar.getEndX() + childsBar.getStartX())/2.0D;
        } else {
            x = MathUtils.clamp(childsBar.getStartX()+minXend, descentBarParentStart.getX(), childsBar.getEndX()-minXend);
        }
        return new Point2D(x, childsBar.getStartY());
    }

    private double calcDescentBarMiddleY(final Point2D descentBarChildsStart, final int nChild) {
        // calculate height of middle descent bar (which is hidden if there's only one child)
        double descentBarMiddleY = descentBarChildsStart.getY();
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





    private class Couple {
        private final boolean exists;
        private final Point2D pt1;
        private final Point2D pt2;

        public Couple(final Indi husb, final Indi wife) {
            this.exists = !(husb == null && wife == null);

            if (!this.exists) {
                // should never happen
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
