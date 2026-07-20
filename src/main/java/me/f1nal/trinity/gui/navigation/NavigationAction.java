package me.f1nal.trinity.gui.navigation;

public enum NavigationAction {
    NAVIGATE("Navigating to", "Navigated to"),
    FOLLOW_MEMBER("Followed member to", "Followed member to"),
    FOLLOW_XREF("Followed xref to", "Followed xref to"),
    FOLLOW_SINGLE_XREF("Followed single xref to", "Followed single xref to");

    private final String notificationPrefix;
    private final String historyPrefix;

    NavigationAction(String notificationPrefix, String historyPrefix) {
        this.notificationPrefix = notificationPrefix;
        this.historyPrefix = historyPrefix;
    }

    public String getNotificationPrefix() {
        return notificationPrefix;
    }

    public String getHistoryPrefix() {
        return historyPrefix;
    }

    public boolean isFollow() {
        return this != NAVIGATE;
    }

}
