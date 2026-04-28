package nu.mine.mosher.genealogy.xy.shape;

// TODO make this immutable?
// TODO remove concept of "start" and "end" points (i.e., direction).
// TODO (re)implement in terms of two Point2D objects instead of four primitives
public class Line {
    private double startX = 0;
    private double startY = 0;
    private double endX = 0;
    private double endY = 0;



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
        this.startX = p.x();
        this.startY = p.y();
    }

    public void setEnd(final Point2D p) {
        this.endX = p.x();
        this.endY = p.y();
    }



    public Point2D getStart() {
        return new Point2D(this.startX, this.startY);
    }

    public Point2D getEnd() {
        return new Point2D(this.endX, this.endY);
    }



    public Point2D midpoint() {
        return new Point2D((this.startX+this.endX)/2.0D, (this.startY+this.endY)/2.0D);
    }

    public double length() {
        return getStart().distance(getEnd());
    }
}
