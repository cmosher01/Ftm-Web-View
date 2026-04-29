package nu.mine.mosher.genealogy.xy.shape;

public final class Bounds {
    private final double x;
    private final double y;
    private final double width;
    private final double height;



    private Bounds(final double x, final double y, final double width, final double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }



    public static Bounds withPosSize(final double x, final double y, final double width, final double height) {
        return new Bounds(x, y, width, height);
    }

    public static Bounds withSides(final double left, final double top, final double right, final double bottom) {
        return new Bounds(left, top, right-left, bottom-top);
    }



    public double x() { return this.x; }

    public double y() { return this.y; }

    public double width() { return this.width; }

    public double height() { return this.height; }



    public double left() { return this.x; }

    public double top() { return this.y; }

    public double right() { return this.x + this.width; }

    public double bottom() { return this.y + this.height; }



    public Bounds outset(final Insets outsets) {
        return new Bounds(
            this.x - outsets.left(),
            this.y - outsets.top(),
            this.width + outsets.left() + outsets.right(),
            this.height + outsets.top() + outsets.bottom());
    }

    public Bounds translate(final double dx, final double dy) {
        return new Bounds(
            this.x + dx,
            this.y + dy,
            this.width,
            this.height);
    }
}
