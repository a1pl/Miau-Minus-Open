package miau.notification;

import miau.util.time.TimerUtil;

public final class Notification {
    private final TimerUtil timer = new TimerUtil();
    private final NotificationType type;
    private final String title;
    private final String description;
    private final int duration;

    Notification(NotificationType type, String title, String description, int duration) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.duration = duration;
        this.timer.reset();
    }

    public NotificationType getType() {
        return this.type;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public int getDuration() {
        return this.duration;
    }

    public boolean hasExpired() {
        return this.timer.hasTimeElapsed(this.duration, false);
    }

    public long getTime() {
        return this.timer.getTime();
    }
}
