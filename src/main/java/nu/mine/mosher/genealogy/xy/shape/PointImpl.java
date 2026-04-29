package nu.mine.mosher.genealogy.xy.shape;

record PointImpl(double x, double y) implements Point {
    @Override
    public double distance(final Point pt) {
        final double a = this.x - pt.x();
        final double b = this.y - pt.y();
        return Math.sqrt(sqr(a) + sqr(b));
    }

    @Override
    public Point scale(final double factor) {
        return Point.create(
            this.x * factor,
            this.y * factor);
    }

    @Override
    public Point translate(final double dx, final double dy) {
        return Point.create(
            this.x + dx,
            this.y + dy);
    }



    private static double sqr(final double x) {
        return x * x;
    }
}
