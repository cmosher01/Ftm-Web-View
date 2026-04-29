package nu.mine.mosher.genealogy.xy.shape;

class LineImpl implements Line {
    private final Point p1;
    private final Point p2;

    LineImpl(final Point p1, final Point p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    @Override
    public Point p1() {
        return this.p1;
    }

    @Override
    public Point p2() {
        return this.p2;
    }

    @Override
    public Point midpoint() {
        return Point.create(
            avg(this.p1.x(), this.p2.x()),
            avg(this.p1.y(), this.p2.y()));
    }

    @Override
    public double length() {
        return this.p1.distance(this.p2);
    }

    @Override
    public Line translate(final double dx, final double dy) {
        return Line.create(
            this.p1.translate(dx, dy),
            this.p2.translate(dx, dy));
    }

    @Override
    public Point section(final double ratio) {
        return Point.create(
            (1 - ratio) * this.p1().x() + ratio * this.p2().x(),
            (1 - ratio) * this.p1().y() + ratio * this.p2().y());
    }


    static class HorzImpl extends LineImpl implements LineImpl.Horz {
        // normalized so x1 is the left (least value), and x2 is the right
        // TODO should we implement a normalize() method instead, and let user decide?
        HorzImpl(final double y, final double x1, final double x2) {
            super(
                Point.create(Math.min(x1, x2), y),
                Point.create(Math.max(x1, x2), y));
        }

        @Override
        public double y() {
            assert p1().y() == p2().y();
            return p1().y();
        }

        @Override
        public double x1() {
            return p1().x();
        }

        @Override
        public double x2() {
            return p2().x();
        }

        @Override
        public Horz translate(final double dy) {
            return Horz.create(y() + dy, x1(), x2());
        }
    }





    static class VertImpl extends LineImpl implements LineImpl.Vert {
        // normalized so y1 is the top (least value), and y2 is the bottom
        // TODO should we implement a normalize() method instead, and let user decide?
        VertImpl(final double x, final double y1, final double y2) {
            super(
                Point.create(x, Math.min(y1, y2)),
                Point.create(x, Math.max(y1, y2)));
        }

        @Override
        public double x() {
            assert p1().x() == p2().x();
            return p1().x();
        }

        @Override
        public double y1() {
            return p1().y();
        }

        @Override
        public double y2() {
            return p2().y();
        }

        @Override
        public Vert translate(final double dx) {
            return Vert.create(x() + dx, y1(), y2());
        }
    }






    private static double avg(final double a, final double b) {
        return (a+b) / 2.0D;
    }
}
