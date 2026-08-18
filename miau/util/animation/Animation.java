package miau.util.animation;

import miau.util.time.TimerUtil;

public class Animation {
    private Easing easing;
    private long millis;
    protected long duration;
    private long startTime;
    private float startValue;
    private float destinationValue;
    private float value;
    private boolean finished;
    public TimerUtil timerUtil = new TimerUtil();
    protected double endPoint;
    protected Direction direction;

    public Animation(Easing easing, long duration) {
        this.easing = easing;
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
        this.startValue = 0.0F;
        this.destinationValue = 0.0F;
        this.value = 0.0F;
        this.finished = true;
    }

    public Animation(int ms, double endPoint) {
        this(ms, endPoint, Direction.FORWARDS);
    }

    public Animation(int ms, double endPoint, Direction direction) {
        this.duration = ms;
        this.endPoint = endPoint;
        this.direction = direction;
    }

    public void run(float destinationValue) {
        this.millis = System.currentTimeMillis();
        if (this.destinationValue != destinationValue) {
            this.destinationValue = destinationValue;
            this.reset();
        } else {
            this.finished = this.millis - this.duration > this.startTime || this.value == destinationValue;
            if (this.finished) {
                this.value = destinationValue;
                return;
            }
        }

        float result = (float)this.easing.apply(this.getProgress());
        if (this.value > destinationValue) {
            this.value = this.startValue - (this.startValue - destinationValue) * result;
        } else {
            this.value = this.startValue + (destinationValue - this.startValue) * result;
        }
    }

    public double getProgress() {
        return (double)(System.currentTimeMillis() - this.startTime) / this.duration;
    }

    public void reset() {
        this.startTime = System.currentTimeMillis();
        this.startValue = this.value;
        this.finished = false;
        if (this.timerUtil != null) {
            this.timerUtil.reset();
        }
    }

    public boolean finished(Direction direction) {
        return this.isDone() && this.direction.equals(direction);
    }

    public double getLinearOutput() {
        return 1.0 - (double)this.timerUtil.getTime() / this.duration * this.endPoint;
    }

    public double getEndPoint() {
        return this.endPoint;
    }

    public void setEndPoint(double endPoint) {
        this.endPoint = endPoint;
    }

    public boolean isDone() {
        return this.timerUtil.hasTimeElapsed(this.duration);
    }

    public void changeDirection() {
        this.setDirection(this.direction.opposite());
    }

    public Direction getDirection() {
        return this.direction;
    }

    public Animation setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            this.timerUtil
                .setTime(
                    System.currentTimeMillis() - (this.duration - Math.min(this.duration, this.timerUtil.getTime()))
                );
        }

        return this;
    }

    protected boolean correctOutput() {
        return false;
    }

    public Double getOutput() {
        if (this.direction.forwards()) {
            return this.isDone()
                ? this.endPoint
                : this.getEquation((double)this.timerUtil.getTime() / this.duration) * this.endPoint;
        } else if (this.isDone()) {
            return 0.0;
        } else if (this.correctOutput()) {
            double revTime = Math.min(this.duration, Math.max(0L, this.duration - this.timerUtil.getTime()));
            return this.getEquation(revTime / this.duration) * this.endPoint;
        } else {
            return (1.0 - this.getEquation((double)this.timerUtil.getTime() / this.duration)) * this.endPoint;
        }
    }

    protected double getEquation(double x) {
        return x;
    }

    public Easing getEasing() {
        return this.easing;
    }

    public void setEasing(Easing easing) {
        this.easing = easing;
    }

    public long getDuration() {
        return this.duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public float getValue() {
        return this.value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public void setStartValue(float startValue) {
        this.startValue = startValue;
        this.value = startValue;
    }

    public float getStartValue() {
        return this.startValue;
    }

    public long getMillis() {
        return this.millis;
    }

    public void setMillis(long millis) {
        this.millis = millis;
    }
}
