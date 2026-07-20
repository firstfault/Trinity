package me.f1nal.trinity.gui.windows.impl.navigation;

import imgui.ImGui;
import imgui.ImDrawList;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiSelectableFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.AccessFlags;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.InputType;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.navigation.NavigationAction;
import me.f1nal.trinity.gui.navigation.NavigationEntry;
import me.f1nal.trinity.gui.navigation.NavigationHistory;
import me.f1nal.trinity.gui.navigation.NavigationTarget;
import me.f1nal.trinity.gui.windows.api.StaticWindow;
import me.f1nal.trinity.theme.CodeColorScheme;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public final class NavigationHistoryWindow extends StaticWindow {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final float CONTENT_Y_OFFSET = 1.5F;

    public NavigationHistoryWindow(Trinity trinity) {
        super("Navigation History", 430.F, 180.F, trinity);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!Main.getWindowManager().isResettingWindows()) {
            Main.getPreferences().setNavigationHistoryVisible(visible);
        }
    }

    @Override
    protected void renderFrame() {
        NavigationHistory history = Main.getDisplayManager().getNavigationHistory();
        drawButton("Back", Main.getKeyBindManager().DECOMPILER_NAVIGATE_BACK.getKeyName(),
                !history.canGoBack(), Main.getDisplayManager()::navigateBack);
        ImGui.sameLine();
        drawButton("Forward", Main.getKeyBindManager().DECOMPILER_NAVIGATE_FORWARD.getKeyName(),
                !history.canGoForward(), Main.getDisplayManager()::navigateForward);
        ImGui.sameLine();
        if (ImGui.button("Clear")) history.clear();
        ImGui.separator();

        if (ImGui.beginChild(getId("NavigationEntries"), 0.F, 0.F)) {
            ImGui.setCursorPosY(ImGui.getCursorPosY() + 2.F);
            List<NavigationEntry> entries = history.getEntries();
            if (entries.isEmpty()) {
                ImGui.textDisabled("No navigation history yet.");
            }
            for (int i = entries.size() - 1; i >= 0; i--) {
                this.drawEntry(entries.get(i), i, i == history.getCurrentIndex());
            }
        }
        ImGui.endChild();
    }

    private void drawEntry(NavigationEntry entry, int index, boolean selected) {
        float rowHeight = ImGui.getTextLineHeight();
        if (ImGui.selectable("###NavigationEntry." + entry.id(), selected,
                ImGuiSelectableFlags.None, Math.max(1.F, ImGui.getContentRegionAvailX()), rowHeight)) {
            Main.getDisplayManager().replayNavigation(index);
        }

        ImVec2 minimum = ImGui.getItemRectMin();
        float textY = minimum.y + (rowHeight - ImGui.getTextLineHeight()) * 0.5F + CONTENT_Y_OFFSET;
        float markerSize = 12.F * Main.getPreferences().getDefaultFont().getSize() / 15.F;
        float markerX = minimum.x + 8.F;
        float markerY = textY + (ImGui.getTextLineHeight() - markerSize) * 0.5F + 1.F;
        float timestampX = markerX + markerSize + 7.F;
        String timestamp = formatTimestamp(entry.timestampMillis());
        float timestampWidth = ImGui.calcTextSize("12:59 AM").x + 9.F;
        float actionX = timestampX + timestampWidth;
        String actionLabel = entry.action().getHistoryLabel();
        float actionWidth = 0.F;
        for (NavigationAction action : NavigationAction.values()) {
            actionWidth = Math.max(actionWidth, ImGui.calcTextSize(action.getHistoryLabel()).x);
        }
        actionWidth += 6.F;
        float targetX = actionX + actionWidth;
        NavigationTarget.ResolvedNavigation resolved = entry.target().resolve(trinity);
        int targetColor = getTargetColor(entry.target(), resolved);
        ImDrawList drawList = ImGui.getWindowDrawList();
        drawList.addRectFilled(markerX, markerY, markerX + markerSize, markerY + markerSize,
                targetColor, 1.5F);
        drawList.addText(timestampX, textY, CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 175), timestamp);
        drawList.addText(actionX, textY, CodeColorScheme.DISABLED, actionLabel);
        drawList.addText(targetX, textY, ImGui.getColorU32(ImGuiCol.Text),
                getTargetDisplayName(entry, resolved));

        if (ImGui.isItemHovered()) {
            this.drawEntryTooltip(entry, resolved, targetColor);
        }
    }

    private void drawEntryTooltip(NavigationEntry entry, NavigationTarget.ResolvedNavigation resolved,
                                  int targetColor) {
        NavigationTarget target = entry.target();
        Input<?> input = resolved == null ? null : resolved.input();
        String targetKind = getTargetKind(target, input);

        ImGui.beginTooltip();
        ImGui.textColored(targetColor, targetKind);
        ImGui.sameLine();
        ImGui.textDisabled("via " + entry.action().getHistoryLabel());
        ImGui.separator();

        drawDetail("Class", toJavaName(target.getClassTarget().getDisplayOrRealName()));
        if (entry.displayText() != null) {
            drawDetail("Constant", entry.displayText());
        }
        String realClassName = toJavaName(target.getClassTarget().getRealName());
        if (!realClassName.equals(toJavaName(target.getClassTarget().getDisplayOrRealName()))) {
            drawDetail("Bytecode class", realClassName);
        }

        if (input instanceof MethodInput method) {
            drawMemberDetails("Method", method, method.getName(), method.getDescriptor());
            drawDetail("Signature", formatMethodSignature(method));
        } else if (input instanceof FieldInput field) {
            drawMemberDetails("Field", field, field.getDetails().getName(), field.getDescriptor());
            drawDetail("Type", formatType(field.getDescriptor()));
        } else if (input instanceof ClassInput classInput) {
            drawDetail("Kind", classInput.getClassTarget().getKind().getFileType());
            drawDetail("Access", formatAccess(classInput));
        } else {
            if (target.getMemberName() != null) drawDetail("Member", target.getMemberName());
            if (target.getMemberDescriptor() != null) drawDetail("Descriptor", target.getMemberDescriptor());
            ImGui.spacing();
            ImGui.textColored(CodeColorScheme.NOTIFY_WARN, "Target is no longer available");
        }

        if (target.isInstructionTarget()) {
            int instructionIndex = getInstructionIndex(resolved, target.getInstructionIndex());
            drawDetail("Usage", instructionIndex < 0 ? "bytecode instruction" : "instruction #" + instructionIndex);
        }
        ImGui.endTooltip();
    }

    private static void drawMemberDetails(String label, Input<?> input, String bytecodeName, String descriptor) {
        String displayName = input.getDisplayName().getName();
        drawDetail(label, displayName);
        if (!displayName.equals(bytecodeName)) drawDetail("Bytecode name", bytecodeName);
        drawDetail("Descriptor", descriptor);
        drawDetail("Access", formatAccess(input));
    }

    private static void drawDetail(String label, String value) {
        ImGui.textDisabled(label);
        ImGui.sameLine(105.F);
        ImGui.text(Objects.requireNonNullElse(value, "<none>"));
    }

    private static int getTargetColor(NavigationTarget target, NavigationTarget.ResolvedNavigation resolved) {
        Input<?> input = resolved == null ? null : resolved.input();
        if (input instanceof MethodInput || target.getInputType() == InputType.METHOD) {
            return CodeColorScheme.METHOD_REF;
        }
        if (input instanceof FieldInput || target.getInputType() == InputType.FIELD) {
            return CodeColorScheme.FIELD_REF;
        }
        return target.getClassTarget().getKind().getColor();
    }

    private static String getTargetKind(NavigationTarget target, Input<?> input) {
        if (input instanceof MethodInput || target.getInputType() == InputType.METHOD) return "Method";
        if (input instanceof FieldInput || target.getInputType() == InputType.FIELD) return "Field";
        return target.getClassTarget().getKind().getFileType();
    }

    private static String formatMethodSignature(MethodInput method) {
        String parameters = Arrays.stream(Type.getArgumentTypes(method.getDescriptor()))
                .map(type -> formatType(type.getDescriptor()))
                .collect(Collectors.joining(", "));
        return formatType(Type.getReturnType(method.getDescriptor()).getDescriptor()) + " "
                + method.getDisplayName().getName() + "(" + parameters + ")";
    }

    private static String formatType(String descriptor) {
        return Type.getType(descriptor).getClassName();
    }

    private static String formatAccess(Input<?> input) {
        List<String> flags = new ArrayList<>();
        for (AccessFlags.Flag flag : AccessFlags.getFlags()) {
            if (input.isAccessFlagValid(flag) && input.getAccessFlags().isFlag(flag)) {
                flags.add(flag.getName().toLowerCase(Locale.ROOT));
            }
        }
        return flags.isEmpty() ? "package" : String.join(" ", flags);
    }

    private static int getInstructionIndex(NavigationTarget.ResolvedNavigation resolved, int fallback) {
        if (resolved == null || !(resolved.input() instanceof MethodInput method)) return fallback;
        AbstractInsnNode instruction = resolved.instruction();
        return instruction == null ? fallback : method.getInstructions().indexOf(instruction);
    }

    private static String toJavaName(String internalName) {
        return internalName.replace('/', '.');
    }

    private static String formatTimestamp(long timestampMillis) {
        return TIME_FORMAT.format(Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()));
    }

    private static String getTargetDisplayName(NavigationEntry entry,
                                               NavigationTarget.ResolvedNavigation resolved) {
        if (entry.displayText() != null) return entry.displayText();
        NavigationTarget target = entry.target();
        Input<?> input = resolved == null ? null : resolved.input();
        if (input instanceof MethodInput method) return method.getDisplayName().getName();
        if (input instanceof FieldInput field) return field.getDisplayName().getName();
        if (input instanceof ClassInput) return target.getDisplayClassName();
        return Objects.requireNonNullElse(target.getMemberName(), target.getDisplayClassName());
    }

    private static void drawButton(String label, String shortcut, boolean disabled, Runnable action) {
        if (disabled) ImGui.beginDisabled();
        if (ImGui.button(label)) action.run();
        if (!disabled && ImGui.isItemHovered()) ImGui.setTooltip(shortcut);
        if (disabled) ImGui.endDisabled();
    }
}
