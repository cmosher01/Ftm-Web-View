package nu.mine.mosher.genealogy.xy.shape;

public class BoundingBox extends Bounds {
    private int hash = 0;

    public BoundingBox(double minX, double minY, double width, double height) {
        super(minX, minY, width, height);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof BoundingBox other) {
            return getMinX() == other.getMinX()
                && getMinY() == other.getMinY()
                && getWidth() == other.getWidth()
                && getHeight() == other.getHeight();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            long bits = 7L;
            bits = 31L * bits + Double.doubleToLongBits(getMinX());
            bits = 31L * bits + Double.doubleToLongBits(getMinY());
            bits = 31L * bits + Double.doubleToLongBits(getWidth());
            bits = 31L * bits + Double.doubleToLongBits(getHeight());
            hash = (int) (bits ^ (bits >> 32));
        }
        return hash;
    }

    @Override
    public String toString() {
        return "BoundingBox ["
                + "minX:" + getMinX()
                + ", minY:" + getMinY()
                + ", width:" + getWidth()
                + ", height:" + getHeight()
                + ", maxX:" + getMaxX()
                + ", maxY:" + getMaxY()
                + "]";
    }

    public Bounds inset(final Insets insets) {
        return new BoundingBox(
                getMinX() - insets.getLeft(),
                getMinY() - insets.getTop(),
                getWidth() + insets.getLeft() + insets.getRight(),
                getHeight() + insets.getTop() + insets.getBottom());
    }

    @Override
    public Bounds translate(final double dx, final double dy) {
        return new BoundingBox(
                getMinX() + dx,
                getMinY() + dy,
                getWidth(),
                getHeight());
    }
}
