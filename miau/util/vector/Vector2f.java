package miau.util.vector;

public final class Vector2f {
    public float x;
    public float y;

    public Vector2f() {
    }

    public Vector2f(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2f(double x, double y) {
        this.x = (float)x;
        this.y = (float)y;
    }

    public Vector2f offset(float x, float y) {
        return new Vector2f(this.x + x, this.y + y);
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public Vector2f copy() {
        return new Vector2f(this.x, this.y);
    }
}
