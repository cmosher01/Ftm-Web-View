package nu.mine.mosher.genealogy.xy.shape;

public class Point2D {
    public static final Point2D ZERO = new Point2D(0.0D, 0.0D);

    private final double x;
    private final double y;



    public Point2D(final double x, final double y) {
        this.x = x;
        this.y = y;
    }

    public final double getX() {
        return this.x;
    }
    public final double getY() {
        return this.y;
    }

    public double distance(final Point2D point) {
        double a = this.x - point.getX();
        double b = this.y - point.getY();
        return Math.sqrt(a * a + b * b);
    }

    public Point2D multiply(final double factor) {
        return new Point2D(this.x * factor, this.y * factor);
    }

    public Point2D translate(final double dx, final double dy) {
        return new Point2D(this.x+dx, this.y+dy);
    }
}
