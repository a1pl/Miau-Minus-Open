package miau.util.time;

public class TimerUtil {
    private long lastMS = 0L;

    public void reset() {
        this.lastMS = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - this.lastMS;
    }

    public boolean hasTimeElapsed(long ms) {
        return this.getElapsedTime() >= ms;
    }

    public boolean hasTimeElapsed(long time, boolean reset) {
        if (System.currentTimeMillis() - this.lastMS > time) {
            if (reset) {
                this.reset();
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean hasTimeElapsed(double time) {
        return this.hasTimeElapsed((long)time);
    }

    public long getTime() {
        return this.getElapsedTime();
    }

    public void setTime() {
        this.lastMS = 0L;
    }

    public void setTime(long time) {
        this.lastMS = time;
    }
}
