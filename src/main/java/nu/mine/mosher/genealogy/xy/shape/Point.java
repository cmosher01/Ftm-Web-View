package nu.mine.mosher.genealogy.xy.shape;

public interface Point {
    Point ZERO = Point.create(0.0D, 0.0D);

    static Point create(final double x, final double y) {
        return new PointImpl(x, y);
    }

    double x();
    double y();
    double distance(final Point pt);
    Point scale(final double factor);
    Point translate(final double dx, final double dy);
}
