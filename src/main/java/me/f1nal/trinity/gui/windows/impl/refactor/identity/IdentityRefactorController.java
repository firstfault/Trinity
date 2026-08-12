package me.f1nal.trinity.gui.windows.impl.refactor.identity;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.events.EventRefreshDecompilerText;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.viewport.notifications.Notification;
import me.f1nal.trinity.gui.viewport.notifications.NotificationType;
import me.f1nal.trinity.gui.viewport.notifications.SimpleCaption;
import me.f1nal.trinity.refactor.identity.IdentityRefactorPlan;
import me.f1nal.trinity.refactor.identity.IdentityRefactorRequest;
import me.f1nal.trinity.refactor.identity.IdentityRefactorResult;
import me.f1nal.trinity.remap.RenameType;

import java.util.List;

/** GUI entry points for identity analysis, conditional review, and application. */
public final class IdentityRefactorController {
    private IdentityRefactorController() {
    }

    public static void promptForIdentityName(Input<?> input) {
        if (input instanceof MethodInput method) {
            if (method.isInit()) {
                input = method.getOwningClass();
            } else if (method.isClinit()) {
                notify(NotificationType.INFO, "Bytecode Rename",
                        "Class initializers always use the JVM name <clinit>.");
                return;
            }
        }
        Input<?> target = input;
        boolean alreadyOpen = Main.getWindowManager().getPopups().stream()
                .filter(popup -> !popup.isCloseRequested())
                .filter(IdentityRenameInputPopup.class::isInstance)
                .map(IdentityRenameInputPopup.class::cast)
                .anyMatch(popup -> popup.targets(target));
        if (alreadyOpen) return;
        Main.getWindowManager().addPopup(new IdentityRenameInputPopup(
                Main.getTrinity(), input, IdentityRefactorRequest.currentName(input)));
    }

    public static void applyDisplayName(Input<?> input) {
        if (input.getDisplayName().getType() == RenameType.NONE) return;
        submit(input, input.getDisplayName().getName(), true);
    }

    static void submit(Input<?> input, String newName, boolean applyingDisplayName) {
        submit(input, newName, applyingDisplayName, null);
    }

    static void submit(Input<?> input, String newName, boolean applyingDisplayName,
                       String expectedName) {
        Trinity trinity = Main.getTrinity();
        IdentityRefactorRequest request;
        try {
            request = IdentityRefactorRequest.forInput(
                    input, newName == null ? "" : newName, applyingDisplayName);
        } catch (Throwable throwable) {
            notify(NotificationType.ERROR, "Bytecode Rename", message(throwable));
            return;
        }
        if (request.name().equals(request.newName())) {
            if (expectedName != null && !expectedName.equals(request.name())) {
                // Another request for this same stable input completed while the dialog
                // was open. The requested result is already present; do not report a
                // misleading no-op warning for the stale dialog.
                return;
            }
            if (applyingDisplayName && clearRedundantDisplayName(trinity, input, request.name())) {
                notify(NotificationType.INFO, "Bytecode Rename",
                        "The display name already matches the bytecode and was cleared.");
            } else {
                notify(NotificationType.INFO, "Bytecode Rename",
                        "The bytecode already uses that name.");
            }
            return;
        }

        trinity.getIdentityRefactorService().analyze(request).whenComplete((plan, failure) ->
                Main.runLater(() -> {
                    if (Main.getTrinity() != trinity) return;
                    if (failure != null) {
                        notify(NotificationType.ERROR, "Bytecode Rename",
                                "Analysis failed: " + message(failure));
                    } else if (plan.requiresReview()) {
                        Main.getWindowManager().addClosableWindow(
                                new IdentityRefactorWindow(trinity, plan));
                    } else {
                        apply(plan);
                    }
                }));
    }

    private static boolean clearRedundantDisplayName(
            Trinity trinity, Input<?> input, String bytecodeName) {
        if (input.getDisplayName().getType() == RenameType.NONE
                || !input.getDisplayName().getName().equals(bytecodeName)) return false;

        IDatabaseSavable<?> savable = input instanceof ClassInput classInput
                ? classInput.getClassTarget()
                : input instanceof IDatabaseSavable<?> databaseSavable
                ? databaseSavable : null;
        if (savable != null) trinity.getDatabase().remove(savable);
        input.getDisplayName().setName(bytecodeName, RenameType.NONE);
        Main.getEventBus().post(new EventRefreshDecompilerText(decompiledClass -> true));
        return true;
    }

    static boolean apply(IdentityRefactorPlan plan) {
        List<me.f1nal.trinity.gui.windows.api.ClosableWindow> affectedWindows =
                Main.getWindowManager().getClosableWindows().stream()
                        .filter(window -> window.isVisible() && !window.isCloseRequested())
                        .filter(window -> window.isAffectedByIdentityRefactor(
                                plan.getAffectedClasses()))
                        .toList();
        List<me.f1nal.trinity.gui.windows.api.ClosableWindow> unsaved = affectedWindows.stream()
                .filter(me.f1nal.trinity.gui.windows.api.ClosableWindow::hasUnsavedChanges)
                .toList();
        if (!unsaved.isEmpty()) {
            String names = unsaved.stream()
                    .map(window -> "- " + window.getUnsavedChangesDescription())
                    .collect(java.util.stream.Collectors.joining("\n"));
            Main.getWindowManager().dialog("Refactor Blocked")
                    .warning("Save or close unsaved editors before changing bytecode identities:\n" + names)
                    .confirm("OK", () -> {
                    })
                    .show();
            return false;
        }
        try {
            IdentityRefactorResult result = Main.getTrinity()
                    .getIdentityRefactorService().apply(plan);
            affectedWindows.forEach(window -> {
                if (window.isVisible() && !window.isCloseRequested()) {
                    window.identityRefactorApplied();
                }
            });
            notify(NotificationType.SUCCESS, "Bytecode Rename",
                    "Updated " + result.changedValueCount() + " classfile value"
                            + (result.changedValueCount() == 1 ? "" : "s") + " across "
                            + result.changedClassCount() + " class"
                            + (result.changedClassCount() == 1 ? "." : "es."));
            return true;
        } catch (Throwable throwable) {
            notify(NotificationType.ERROR, "Bytecode Rename", message(throwable));
            return false;
        }
    }

    private static void notify(NotificationType type, String title, String message) {
        Main.getDisplayManager().addNotification(new Notification(type,
                new SimpleCaption(title), ColoredStringBuilder.create().fmt("{}", message).get()));
    }

    private static String message(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getMessage() == null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
