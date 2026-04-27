package nu.mine.mosher.genealogy.xy.shape;

public final class Bounds {
    private final double minX;
    private final double minY;
    private final double width;
    private final double height;

    public Bounds(double minX, double minY, double width, double height) {
        this.minX = minX;
        this.minY = minY;
        this.width = width;
        this.height = height;
    }

    public double getMinX() { return this.minX; }

    public double getMinY() { return this.minY; }

    public double getWidth() { return this.width; }

    public double getHeight() { return this.height; }

    public double getMaxX() { return this.minX + this.width; }

    public double getMaxY() { return this.minY + this.height; }

    public Bounds inset(final Insets insets) {
        return new Bounds(
            this.minX - insets.getLeft(),
            this.minY - insets.getTop(),
            this.width + insets.getLeft() + insets.getRight(),
            this.height + insets.getTop() + insets.getBottom());
    }

    public Bounds translate(final double dx, final double dy) {
        return new Bounds(
            this.minX + dx,
            this.minY + dy,
            this.width,
            this.height);
    }
}
