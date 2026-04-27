package nu.mine.mosher.genealogy.xy.shape;

public abstract class Bounds {
    private final double minX;
    private final double minY;
    private final double width;
    private final double height;
    private final double maxX;
    private final double maxY;

    protected Bounds(double minX, double minY, double width, double height) {
        this.minX = minX;
        this.minY = minY;
        this.width = width;
        this.height = height;
        this.maxX = minX + width;
        this.maxY = minY + height;
    }

    public final double getMinX() { return this.minX; }

    public final double getMinY() { return this.minY; }

    public final double getWidth() { return this.width; }

    public final double getHeight() { return this.height; }

    public final double getMaxX() { return this.maxX; }

    public final double getMaxY() { return this.maxY; }

    public abstract Bounds inset(final Insets insets);

    public abstract Bounds translate(final double dx, final double dy);
}
