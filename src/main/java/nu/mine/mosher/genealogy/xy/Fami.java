package nu.mine.mosher.genealogy.xy;

import nu.mine.mosher.genealogy.xy.metrics.FontBasedMetrics;
import nu.mine.mosher.genealogy.xy.shape.*;

import java.util.*;

public class Fami {
    private static final double MIN_DISTANCE = 1.51D;

    private final FontBasedMetrics metricsFont;

    private Indi husb;
    private Indi wife;
    private final List<Indi> rChild = new ArrayList<>();

    private Line parentBar1;
    private Line parentBar2;
//    private final List<StackPane> phantomPanes = new ArrayList<>(0);

    private Line descentBarParent;
    private Line descentBarMiddle;
    private Line descentBarChilds;

    private Line childBar;
    private Line[] rChildBar;



    public Fami(final FontBasedMetrics metricsFont) {
        this.metricsFont = metricsFont;
    }



    public void setHusb(final Indi indi) {
        husb = indi;
    }

    public void setWife(final Indi indi) {
        wife = indi;
    }

    public void addChild(final Indi indi) {
        if (Objects.nonNull(indi)) {
            rChild.add(indi);
        }
    }


/*

                                  parentBar1              v
                    -----------------------------       <------
                  hu*sb                       wi*fe     metrics.barHeight
                    ------*----------------------       <------
    descentBarParentStart |       parentBar2              ^
                          |
                          | < descentBarParent
                          |
                          |               v descentBarMiddle v
                          *---------------------------------------------*
                                                                        |
                                                                        | < descentBarChilds
                                                                        |
                                                  descentBarChildsStart |       childBar          v
                                                      +-----------+-----*-----------------+     <------
                                           rChildBar: |[0]        |[1]                 [2]|     childHeight
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
        if (husb == null && wife == null && rChild.isEmpty()) {
            return;
        }

        final Couple couple = new Couple(husb, wife);

        // calculate "===" marriage/parent bar
        if (couple.exists) {
            parentBar1 = new Line();
            parentBar1.setStartX(couple.pt1x);
            parentBar1.setStartY(couple.pt1y - this.metricsFont.getBarHeight());
            parentBar1.setEndX(couple.pt2x);
            parentBar1.setEndY(couple.pt2y - this.metricsFont.getBarHeight());

            parentBar2 = new Line();
            parentBar2.setStartX(couple.pt1x);
            parentBar2.setStartY(couple.pt1y + this.metricsFont.getBarHeight());
            parentBar2.setEndX(couple.pt2x);
            parentBar2.setEndY(couple.pt2y + this.metricsFont.getBarHeight());
        }

        if (!rChild.isEmpty()) {
            childBar = new Line();
            childBar.setStartX(rChild.stream().mapToDouble(Indi::x).min().orElseThrow());
            childBar.setEndX(rChild.stream().mapToDouble(Indi::x).max().orElseThrow());

            final double topChildPlaque = this.rChild.stream().map(Indi::getBounds).mapToDouble(Bounds::getMinY).min().orElseThrow();
            childBar.setY(topChildPlaque - this.metricsFont.getChildHeight());

            rChildBar = new Line[rChild.size()];
            for (int i = 0; i < rChildBar.length; i++) {
                final var c = rChild.get(i);

                rChildBar[i] = new Line();

                rChildBar[i].setX(c.x());
                rChildBar[i].setStartY(childBar.getStartY());
                rChildBar[i].setEndY(c.y());
            }








            if (couple.exists) {
                final Point2D descentBarParentStart;
                {
                    // midpoint of child bar
                    final var child = new Point2D((childBar.getStartX() + childBar.getEndX()) / 2.0D, childBar.getStartY());
                    // midpoints of parents
                    final var p1 = new Point2D(couple.pt1x, couple.pt1y);
                    final var p2 = new Point2D(couple.pt2x, couple.pt2y);
                    // figure out which parent is closest to the child bar midpoint
                    descentBarParentStart = child.distance(p1) < child.distance(p2) ? ptParentDescentBar(p1, p2) : ptParentDescentBar(p2, p1);
                }

                // TODO not sure if font-based child height makes sense here, but it looks OK visually
                // Maybe try a fraction of ChartMetrics.medianMinimumDistanceToNeighborScaled() ? 1/2 or 1/4?
                final double aSmallDistance = this.metricsFont.getChildHeight();

                final Point2D descentBarChildsStart;
                {
                    final double offset = aSmallDistance;
                    final double x;
                    if (childBar.getEndX()-childBar.getStartX() < offset*2.0D) {
                        x = (childBar.getEndX()+childBar.getStartX())/2.0D;
                    } else {
                        x = clamp(childBar.getStartX()+offset, descentBarParentStart.getX(), childBar.getEndX()-offset);
                    }
                    descentBarChildsStart = new Point2D(x, childBar.getStartY());
                }

                final double descentBarMiddleY = descentBarChildsStart.getY() - (rChild.size() < 2 ? 0.0D : aSmallDistance/2.0D);

                descentBarChilds = new Line();
                descentBarChilds.setStart(descentBarChildsStart);
                descentBarChilds.setEndX(descentBarChilds.getStartX());
                descentBarChilds.setEndY(descentBarMiddleY);

                descentBarParent = new Line();
                descentBarParent.setStart(descentBarParentStart);
                descentBarParent.setEndX(descentBarParent.getStartX());
                descentBarParent.setEndY(descentBarMiddleY);

                descentBarMiddle = new Line();
                descentBarMiddle.setStart(descentBarChilds.getEnd());
                descentBarMiddle.setEnd(descentBarParent.getEnd());
            }
        }
    }



    public void saveSvg(final SvgBuilder svg) {
        svg.addLine(this.parentBar1);
        svg.addLine(this.parentBar2);
        svg.addLine(this.descentBarParent);
        svg.addLine(this.descentBarMiddle);
        svg.addLine(this.descentBarChilds);
        svg.addLine(this.childBar);
        if (!this.rChild.isEmpty()) {
            Arrays.asList(this.rChildBar).forEach(svg::addLine);
        }
    }








    private Point2D ptParentDescentBar(final Point2D ptNear, final Point2D ptFar) {
        final Point2D ptStart = new Point2D(ptNear.getX(), ptNear.getY() + this.metricsFont.getBarHeight());
        final Point2D ptEnd = new Point2D(ptFar.getX(), ptFar.getY() + this.metricsFont.getBarHeight());
        final double pd = Math.max(ptStart.distance(ptEnd), MIN_DISTANCE);

        final var widthNominal = this.metricsFont.maxWidthPlaque();
        final var dt = pd < 6.0D*widthNominal ? 0.5D : widthNominal/pd;
        return new Point2D((1 - dt) * ptStart.getX() + dt * ptEnd.getX(), (1 - dt) * ptStart.getY() + dt * ptEnd.getY());
    }




    private static double clamp(final double min, final double n, final double max) {
        if (max < min) {
            return n;
        }
        if (n < min) {
            return min;
        }
        if (max < n) {
            return max;
        }
        return n;
    }






    private class Couple {
        public final double pt1x;
        public final double pt1y;
        public final double pt2x;
        public final double pt2y;
        public final boolean exists;

        public Couple(final Indi indi1, final Indi indi2) {
            exists = !(indi1 == null && indi2 == null);

            if (!exists) {
                // don't create TWO phantom parents
                pt1x = pt1y = pt2x = pt2y = Double.NEGATIVE_INFINITY; // TODO ??? what to do here?
            } else if (indi1 == null) {
                pt2x = indi2.x();
                pt2y = indi2.y();
//                final Circle phantom = createPhantom();
                pt1x = pt2x - Fami.this.metricsFont.maxWidthPlaque();
                pt1y = pt2y;
            } else if (indi2 == null) {
                pt1x = indi1.x();
                pt1y = indi1.y();
//                final Circle phantom = createPhantom();
                pt2x = pt1x + Fami.this.metricsFont.maxWidthPlaque();
                pt2y = pt1y;
            } else {
                pt1x = indi1.x();
                pt1y = indi1.y();
                pt2x = indi2.x();
                pt2y = indi2.y();
            }
        }

        // TODO: Create phantoms? Can we make them as Indi objects, somehow?
//        private Circle createPhantom() {
//            final Circle phantom = new Circle(0D, Color.TRANSPARENT);
//
//            final Text textshape = new Text();
//            textshape.setFill(metrics.colors().indiText());
//            textshape.setFont(metrics.font());
//            textshape.setTextAlignment(TextAlignment.CENTER);
//            textshape.setText("\u00A0?\u00A0");
//            new Scene(new Group(textshape));
//            textshape.applyCss();
//            final double inset = metrics.fontSize() / 2.0D;
//            final double w = textshape.getLayoutBounds().getWidth() + inset * 2.0D;
//            final double h = textshape.getLayoutBounds().getHeight() + inset * 2.0D;
//
//            final StackPane plaque = new StackPane();
//            phantomPanes.add(plaque);
//            plaque.setBackground(new Background(new BackgroundFill(metrics.colors().indiBg(), CORNERS, Insets.EMPTY)));
//            plaque.setBorder(new Border(new BorderStroke(metrics.colors().indiBorder(), BorderStrokeStyle.SOLID, CORNERS, BorderWidths.DEFAULT)));
//            StackPane.setMargin(textshape, new Insets(inset));
//            plaque.getChildren().addAll(textshape);
//
//            plaque.layoutXProperty().bind(phantom.layoutXProperty().subtract(w / 2.0D));
//            plaque.layoutYProperty().bind(phantom.layoutYProperty().subtract(h / 2.0D));
//
//            return phantom;
//        }
    }
}
