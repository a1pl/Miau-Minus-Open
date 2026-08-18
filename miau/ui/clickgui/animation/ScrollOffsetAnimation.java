package miau.ui.clickgui.animation;

import miau.util.animation.Easing;

public class ScrollOffsetAnimation {
    private float from;
    private float to;
    private long startMs;
    private final long durationMs;
    private final Easing easing;
    private boolean finished;

    public ScrollOffsetAnimation(long durationMs) {
        this(durationMs, Easing.EASE_OUT_EXPO);
    }

    public ScrollOffsetAnimation(long durationMs, Easing easing) {
        this.durationMs = durationMs;
        this.easing = easing;
        this.from = 0.0F;
        this.to = 0.0F;
        this.startMs = 0L;
        this.finished = true;
    }

    public void reset(float value) {
        this.from = value;
        this.to = value;
        this.startMs = 0L;
        this.finished = true;
    }

    public void setTarget(float newTarget) {
        this.from = this.getValue();
        this.to = newTarget;
        this.startMs = System.currentTimeMillis();
        this.finished = false;
    }

    public void extend(float delta) {
        this.from = this.getValue();
        this.to += delta;
        this.startMs = System.currentTimeMillis();
        this.finished = false;
    }

    public void clampTarget(float min, float max) {
        this.to = Math.max(min, Math.min(max, this.to));
    }

    public float getValue() {
        if (this.startMs == 0L) {
            return this.to;
        } else {
            long elapsed = System.currentTimeMillis() - this.startMs;
            if (elapsed >= this.durationMs) {
                this.startMs = 0L;
                this.from = this.to;
                this.finished = true;
                return this.to;
            } else {
                float t = (float)elapsed / (float)this.durationMs;
                float eased = (float)this.easing.apply(t);
                return this.from + (this.to - this.from) * eased;
            }
        }
    }

    public boolean isAnimating() {
        return !this.finished;
    }

    public float getTarget() {
        return this.to;
    }

    public Easing getEasing() {
        return this.easing;
    }

    public boolean isFinished() {
        return this.finished;
    }
}
