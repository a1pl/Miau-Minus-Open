package miau.management.drag;

public class Snap {
    public double position;
    public double distance;
    public Orientation orientation;
    public boolean center;
    public boolean right;
    public boolean left;

    public Snap(double position, double distance, Orientation orientation, boolean center, boolean right, boolean left) {
        this.position = position;
        this.distance = distance;
        this.orientation = orientation;
        this.center = center;
        this.right = right;
        this.left = left;
    }
}
