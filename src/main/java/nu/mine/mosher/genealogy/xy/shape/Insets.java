package nu.mine.mosher.genealogy.xy.shape;

public class Insets {
    private final double left;
    private final double right;
    private final double top;
    private final double bottom;

    public Insets(final double left, final double right, final double top, final double bottom) {
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    public Insets(final double insets) {
        this(insets, insets, insets, insets);
    }

    public double getLeft() {
        return this.left;
    }

    public double getTop() {
        return this.top;
    }

    public double getRight() {
        return this.right;
    }

    public double getBottom() {
        return this.bottom;
    }
}
