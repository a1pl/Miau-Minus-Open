package miau.notification;

import java.util.ArrayList;
import java.util.List;

public final class NotificationManager {
    private final List<Notification> notifications = new ArrayList<>();

    public List<Notification> getNotifications() {
        return this.notifications;
    }

    public NotificationManager.NotificationBuilder builder(NotificationType type) {
        return new NotificationManager.NotificationBuilder(this, type);
    }

    private Notification publish(Notification notification) {
        System.out.println(notification.getTitle() + ": " + notification.getDescription());
        this.notifications.add(notification);
        return notification;
    }

    public void remove(Notification notification) {
        this.notifications.remove(notification);
    }

    public void pop(String title, String description, NotificationType type) {
        if (!this.hasDuplicate(description)) {
            this.publish(new Notification(type, title, description, 2000));
        }
    }

    public void pop(String description, NotificationType type) {
        this.pop(type.name(), description, type);
    }

    public void pop(String title, String description, int delay, NotificationType type) {
        if (!this.hasDuplicate(description)) {
            this.publish(new Notification(type, title, description, delay));
        }
    }

    public void pop(String description, int delay, NotificationType type) {
        this.pop(type.name(), description, delay, type);
    }

    private boolean hasDuplicate(String description) {
        for (Notification notification : this.notifications) {
            if (notification.getDescription().equalsIgnoreCase(description)) {
                return true;
            }
        }

        return false;
    }

    public static class NotificationBuilder {
        private final NotificationManager dispatcher;
        private final NotificationType type;
        private String title;
        private String description;
        private int duration;

        private NotificationBuilder(NotificationManager dispatcher, NotificationType type) {
            this.dispatcher = dispatcher;
            this.type = type;
            this.title = "Notification";
            this.duration = 2000;
        }

        public NotificationManager.NotificationBuilder title(String title) {
            this.title = title;
            return this;
        }

        public NotificationManager.NotificationBuilder description(String description) {
            this.description = description;
            return this;
        }

        public NotificationManager.NotificationBuilder duration(int duration) {
            this.duration = duration;
            return this;
        }

        public Notification buildAndPublish() {
            return this.dispatcher.publish(new Notification(this.type, this.title, this.description, this.duration));
        }
    }
}
