package miau.notification;

public enum NotificationType {
    SUCCESS("\ue5ca", -13710223),
    ERROR("\ue5cd", -1618884),
    WARN("\ue002", -142795),
    INFO("\ue88f", -9398321);

    private final String icon;
    private final int iconColor;

    NotificationType(String icon, int iconColor) {
        this.icon = icon;
        this.iconColor = iconColor;
    }

    public String getIcon() {
        return this.icon;
    }

    public int getIconColor() {
        return this.iconColor;
    }
}
