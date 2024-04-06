package me.f1nal.trinity.gui.viewport.notifications;

import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.function.Supplier;

public class NotificationType {
    private final String icon;
    private final Supplier<Integer> color;

    public NotificationType(String icon, Supplier<Integer> color) {
        this.icon = icon;
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public int getColor() {
        return color.get();
    }

    public static final NotificationType ERROR = new NotificationType(FontAwesomeIcons.Times, () -> CodeColorScheme.NOTIFY_ERROR);
    public static final NotificationType WARNING = new NotificationType(FontAwesomeIcons.ExclamationTriangle, () -> CodeColorScheme.NOTIFY_WARN);
    public static final NotificationType INFO = new NotificationType(FontAwesomeIcons.Info, () -> CodeColorScheme.NOTIFY_INFORMATION);
    public static final NotificationType SUCCESS = new NotificationType(FontAwesomeIcons.Check, () -> CodeColorScheme.NOTIFY_SUCCESS);
}
