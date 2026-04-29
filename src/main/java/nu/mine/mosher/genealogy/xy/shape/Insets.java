package nu.mine.mosher.genealogy.xy.shape;

public final class Insets {
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



    public double left() {
        return this.left;
    }

    public double top() {
        return this.top;
    }

    public double right() {
        return this.right;
    }

    public double bottom() {
        return this.bottom;
    }
}
