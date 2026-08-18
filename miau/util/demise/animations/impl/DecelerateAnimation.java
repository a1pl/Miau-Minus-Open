package miau.util.demise.animations.impl;

import miau.util.demise.animations.Animation;
import miau.util.demise.animations.Direction;

public class DecelerateAnimation extends Animation {
    public DecelerateAnimation(int ms, double endPoint) {
        super(ms, endPoint);
    }

    public DecelerateAnimation(int ms, double endPoint, Direction direction) {
        super(ms, endPoint, direction);
    }

    @Override
    protected double getEquation(double x) {
        return 1.0 - (x - 1.0) * (x - 1.0);
    }
}
