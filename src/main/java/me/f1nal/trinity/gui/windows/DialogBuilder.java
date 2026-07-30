package me.f1nal.trinity.gui.windows;

import imgui.ImGui;
import imgui.flag.ImGuiKey;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.components.FontAwesomeIcons;
import me.f1nal.trinity.gui.windows.api.PopupWindow;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds a simple modal dialog without requiring a dedicated {@link PopupWindow}
 * subclass.
 *
 * <pre>{@code
 * windowManager.dialog("Remove item?")
 *         .message("This cannot be undone.")
 *         .confirm("Remove", removeAction)
 *         .show();
 * }</pre>
 */
public final class DialogBuilder {
    private final WindowManager windowManager;
    private final Trinity trinity;
    private final String title;
    private final List<DialogMessage> messages = new ArrayList<>();
    private final List<DialogAction> actions = new ArrayList<>();
    private DialogAction cancelAction = new DialogAction("Cancel", () -> {
    }, false);
    private boolean hasPrimaryAction;
    private boolean hasDefaultAction;
    private boolean shown;

    DialogBuilder(WindowManager windowManager, Trinity trinity, String title) {
        this.windowManager = Objects.requireNonNull(windowManager, "windowManager");
        this.trinity = trinity;
        this.title = requireText(title, "title");
    }

    public DialogBuilder message(String message) {
        return this.addMessage(DialogMessageStyle.NORMAL, message);
    }

    public DialogBuilder warning(String message) {
        return this.addMessage(DialogMessageStyle.WARNING, message);
    }

    public DialogBuilder error(String message) {
        return this.addMessage(DialogMessageStyle.ERROR, message);
    }

    public DialogBuilder confirm(String label, Runnable action) {
        return this.addPrimaryAction(label, action, false);
    }

    public DialogBuilder action(String label, Runnable action) {
        return this.addAction(label, action, false);
    }

    public DialogBuilder defaultAction(String label, Runnable action) {
        if (this.hasDefaultAction) {
            throw new IllegalStateException("A dialog can only have one default action");
        }
        return this.addPrimaryAction(label, action, true);
    }

    private DialogBuilder addPrimaryAction(String label, Runnable action,
                                           boolean defaultAction) {
        this.ensurePrimaryActionAvailable();
        String checkedLabel = requireText(label, "label");
        Runnable checkedAction = Objects.requireNonNull(action, "action");
        this.hasPrimaryAction = true;
        this.hasDefaultAction = defaultAction;
        this.actions.add(new DialogAction(checkedLabel, checkedAction,
                defaultAction));
        return this;
    }

    public DialogBuilder cancel(Runnable action) {
        return this.cancel("Cancel", action);
    }

    public DialogBuilder cancel(String label, Runnable action) {
        this.ensureMutable();
        this.cancelAction = new DialogAction(requireText(label, "label"),
                Objects.requireNonNull(action, "action"), false);
        return this;
    }

    /**
     * Enqueues the dialog and prevents further mutation of this builder.
     *
     * @return the popup instance, which may be closed programmatically
     */
    public PopupWindow show() {
        this.ensureMutable();
        if (this.actions.isEmpty()) {
            throw new IllegalStateException(
                    "A dialog must have at least one non-cancel action");
        }

        this.shown = true;
        ActionDialog dialog = new ActionDialog(this.trinity, this.title,
                List.copyOf(this.messages), List.copyOf(this.actions),
                this.cancelAction);
        this.windowManager.addPopup(dialog);
        return dialog;
    }

    private DialogBuilder addMessage(DialogMessageStyle style, String message) {
        this.ensureMutable();
        this.messages.add(new DialogMessage(style, requireText(message, "message")));
        return this;
    }

    private DialogBuilder addAction(String label, Runnable action,
                                    boolean defaultAction) {
        this.ensureMutable();
        this.actions.add(new DialogAction(requireText(label, "label"),
                Objects.requireNonNull(action, "action"), defaultAction));
        return this;
    }

    private void ensureMutable() {
        if (this.shown) {
            throw new IllegalStateException("This dialog has already been shown");
        }
    }

    private void ensurePrimaryActionAvailable() {
        this.ensureMutable();
        if (this.hasPrimaryAction) {
            throw new IllegalStateException(
                    "A dialog can only have one primary action");
        }
    }

    private static String requireText(String value, String name) {
        String result = Objects.requireNonNull(value, name).trim();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return result;
    }

    enum DialogMessageStyle {
        NORMAL,
        WARNING,
        ERROR
    }

    record DialogMessage(DialogMessageStyle style, String text) {
    }

    record DialogAction(String label, Runnable callback, boolean defaultAction) {
    }
}

final class ActionDialog extends PopupWindow {
    private static final float MINIMUM_CONTENT_WIDTH = 320.F;
    private static final float MAXIMUM_CONTENT_WIDTH = 520.F;

    private final List<DialogBuilder.DialogMessage> messages;
    private final List<DialogBuilder.DialogAction> actions;
    private final DialogBuilder.DialogAction cancelAction;
    private boolean completed;

    ActionDialog(Trinity trinity, String title,
                 List<DialogBuilder.DialogMessage> messages,
                 List<DialogBuilder.DialogAction> actions,
                 DialogBuilder.DialogAction cancelAction) {
        super(title, trinity);
        this.messages = messages;
        this.actions = actions;
        this.cancelAction = cancelAction;
    }

    @Override
    protected void renderFrame() {
        ImGui.dummy(MINIMUM_CONTENT_WIDTH, 0.F);
        float wrapPosition = ImGui.getCursorPosX() + MAXIMUM_CONTENT_WIDTH;
        for (int index = 0; index < this.messages.size(); index++) {
            if (index != 0) ImGui.spacing();
            this.drawMessage(this.messages.get(index), wrapPosition);
        }
        if (!this.messages.isEmpty()) ImGui.spacing();

        DialogBuilder.DialogAction defaultAction = null;
        for (int index = 0; index < this.actions.size(); index++) {
            if (index != 0) ImGui.sameLine();
            DialogBuilder.DialogAction action = this.actions.get(index);
            if (ImGui.button(action.label() + "###"
                    + this.getStrId("DialogAction" + index))) {
                this.complete(action);
            }
            if (action.defaultAction()) {
                ImGui.setItemDefaultFocus();
                defaultAction = action;
            }
        }

        if (!this.actions.isEmpty()) ImGui.sameLine();
        if (ImGui.button(this.cancelAction.label() + "###"
                + this.getStrId("DialogCancel"))) {
            this.complete(this.cancelAction);
        }

        if (this.isKeyboardInputReady() && defaultAction != null
                && !ImGui.isAnyItemActive()
                && (ImGui.isKeyPressed(ImGuiKey.Enter, false)
                || ImGui.isKeyPressed(ImGuiKey.KeypadEnter, false))) {
            this.complete(defaultAction);
        }
    }

    private void drawMessage(DialogBuilder.DialogMessage message,
                             float wrapPosition) {
        ImGui.pushTextWrapPos(wrapPosition);
        switch (message.style()) {
            case NORMAL -> ImGui.textWrapped(message.text());
            case WARNING -> ImGui.textColored(CodeColorScheme.NOTIFY_WARN,
                    FontAwesomeIcons.ExclamationTriangle + " " + message.text());
            case ERROR -> ImGui.textColored(CodeColorScheme.NOTIFY_ERROR,
                    FontAwesomeIcons.TimesCircle + " " + message.text());
        }
        ImGui.popTextWrapPos();
    }

    @Override
    protected void onEscape() {
        this.complete(this.cancelAction);
    }

    private void complete(DialogBuilder.DialogAction action) {
        if (this.completed) return;
        this.completed = true;
        this.close();
        action.callback().run();
    }
}
