package miau.util.demise.animations;

import miau.util.demise.math.TimerUtils;

public abstract class Animation {
    public TimerUtils timerUtils = new TimerUtils();
    protected int duration;
    protected double endPoint;
    protected Direction direction;

    public Animation(int ms, double endPoint) {
        this(ms, endPoint, Direction.FORWARDS);
    }

    public Animation(int ms, double endPoint, Direction direction) {
        this.duration = ms;
        this.endPoint = endPoint;
        this.direction = direction;
    }

    public boolean finished(Direction direction) {
        return this.isDone() && this.direction.equals(direction);
    }

    public double getLinearOutput() {
        return 1.0 - (double)this.timerUtils.getTime() / this.duration * this.endPoint;
    }

    public void reset() {
        this.timerUtils.reset();
    }

    public boolean isDone() {
        return this.timerUtils.hasTimeElapsed(this.duration);
    }

    public void changeDirection() {
        this.setDirection(this.direction.opposite());
    }

    public Animation setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            this.timerUtils
                .setTime(
                    System.currentTimeMillis() - (this.duration - Math.min(this.duration, this.timerUtils.getTime()))
                );
        }

        return this;
    }

    protected boolean correctOutput() {
        return false;
    }

    public double getOutput() {
        if (this.direction.forwards()) {
            return this.isDone()
                ? this.endPoint
                : this.getEquation((double)this.timerUtils.getTime() / this.duration) * this.endPoint;
        } else if (this.isDone()) {
            return 0.0;
        } else if (this.correctOutput()) {
            double revTime = Math.min(this.duration, Math.max(0L, this.duration - this.timerUtils.getTime()));
            return this.getEquation(revTime / this.duration) * this.endPoint;
        } else {
            return (1.0 - this.getEquation((double)this.timerUtils.getTime() / this.duration)) * this.endPoint;
        }
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public double getEndPoint() {
        return this.endPoint;
    }

    public void setEndPoint(double endPoint) {
        this.endPoint = endPoint;
    }

    public Direction getDirection() {
        return this.direction;
    }

    protected abstract double getEquation(double var1);
}
