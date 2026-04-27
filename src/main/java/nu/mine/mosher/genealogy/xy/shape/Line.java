package nu.mine.mosher.genealogy.xy.shape;

public class Line {
    private double startX;
    private double startY;
    private double endX;
    private double endY;

    public Line() {
        this(0,0,0,0);
    }

    public Line(final double startX, final double startY, final double endX, final double endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public double getStartX() {
        return this.startX;
    }

    public void setStartX(final double startX) {
        this.startX = startX;
    }

    public double getStartY() {
        return this.startY;
    }

    public void setStartY(final double startY) {
        this.startY = startY;
    }

    public double getEndX() {
        return this.endX;
    }

    public void setEndX(final double endX) {
        this.endX = endX;
    }

    public double getEndY() {
        return this.endY;
    }

    public void setEndY(final double endY) {
        this.endY = endY;
    }

    public void setX(final double x) {
        this.startX = x;
        this.endX = x;
    }

    public void setY(final double y) {
        this.startY = y;
        this.endY = y;
    }

    public void setStart(final Point2D p) {
        this.startX = p.getX();
        this.startY = p.getY();
    }

    public void setEnd(final Point2D p) {
        this.endX = p.getX();
        this.endY = p.getY();
    }

    public Point2D getStart() {
        return new Point2D(this.startX, this.startY);
    }

    public Point2D getEnd() {
        return new Point2D(this.endX, this.endY);
    }
}
