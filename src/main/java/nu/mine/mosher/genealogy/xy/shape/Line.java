package nu.mine.mosher.genealogy.xy.shape;

public interface Line {
    static Line create(final Point p1, final Point p2) {
        return new LineImpl(p1, p2);
    }
    Point p1();
    Point p2();
    Point midpoint();
    double length();
    Line translate(double dx, double dy);
    Point section(double ratio);

    interface Horz extends Line {
        static Horz create(final double y, final double xLeft, final double xRight) {
            return new LineImpl.HorzImpl(y, xLeft, xRight);
        }
        double y();
        double x1();
        double x2();
        Horz translate(double dy);
    }

    interface Vert extends Line {
        static Vert create(final double x, final double yTop, final double yBottom) {
            return new LineImpl.VertImpl(x, yTop, yBottom);
        }
        double x();
        double y1();
        double y2();
        Vert translate(double dx);
    }
}
