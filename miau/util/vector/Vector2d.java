package miau.util.vector;

public final class Vector2d {
    public double x;
    public double y;

    public Vector2d() {
    }

    public Vector2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2d offset(double x, double y) {
        return new Vector2d(this.x + x, this.y + y);
    }

    public Vector2d offset(Vector2d xy) {
        return this.offset(xy.x, xy.y);
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }
}
