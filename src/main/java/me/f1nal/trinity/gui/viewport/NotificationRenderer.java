package me.f1nal.trinity.gui.viewport;

import imgui.ImGui;
import imgui.ImGuiViewport;
import me.f1nal.trinity.gui.viewport.notifications.Notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationRenderer {
    private final List<Notification> notifications = new ArrayList<>();

    public void add(Notification notification) {
        synchronized (this.notifications) {
            this.notifications.add(notification);
        }
    }

    public void draw() {
        Notification[] notifications;

        synchronized (this.notifications) {
            if (this.notifications.isEmpty()) {
                return;
            }
            notifications = this.notifications.toArray(new Notification[0]);
        }

        ImGuiViewport viewport = ImGui.getMainViewport();
        float height = 0.F;

        for (Notification notification : notifications) {
            float thisHeight = 0;
            if (notification.setBounds(height, viewport) || (thisHeight = notification.render()) == -1.F) {
                synchronized (this.notifications) {
                    this.notifications.remove(notification);
                }
            }
            height += thisHeight + 10.F;
        }
    }
}
