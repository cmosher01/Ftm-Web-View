package nu.mine.mosher.genealogy.xy.shape;

public class Point2D {
    public static final Point2D ZERO = new Point2D(0.0D, 0.0D);

    private final double x;
    private final double y;
    private int hash = 0;



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

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Point2D that) {
            return this.getX() == that.getX() && this.getY() == that.getY();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        if (this.hash == 0) {
            long bits = 7L;
            bits = 31L * bits + Double.doubleToLongBits(getX());
            bits = 31L * bits + Double.doubleToLongBits(getY());
            this.hash = (int) (bits ^ (bits >> 32));
        }
        return this.hash;
    }

    @Override
    public String toString() {
        return "Point2D [x = " + getX() + ", y = " + getY() + "]";
    }
}
