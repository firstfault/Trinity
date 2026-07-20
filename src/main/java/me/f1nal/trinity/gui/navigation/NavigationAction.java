package me.f1nal.trinity.gui.navigation;

public enum NavigationAction {
    NAVIGATE("Navigating to", "go to"),
    FOLLOW_MEMBER("Followed member to", "go to"),
    FOLLOW_XREF("Followed xref to", "xref"),
    FOLLOW_SINGLE_XREF("Followed single xref to", "xref");

    private final String notificationPrefix;
    private final String historyLabel;

    NavigationAction(String notificationPrefix, String historyLabel) {
        this.notificationPrefix = notificationPrefix;
        this.historyLabel = historyLabel;
    }

    public String getNotificationPrefix() {
        return notificationPrefix;
    }

    public String getHistoryLabel() {
        return historyLabel;
    }

    public boolean isFollow() {
        return this != NAVIGATE;
    }

}
