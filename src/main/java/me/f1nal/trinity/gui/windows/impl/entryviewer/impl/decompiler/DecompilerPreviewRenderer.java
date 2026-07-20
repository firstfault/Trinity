package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.DecompiledClass;
import me.f1nal.trinity.decompiler.modules.decompiler.exps.VarExprent;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.windows.impl.classstructure.ClassStructureSignatureFormatter;
import me.f1nal.trinity.theme.CodeColorScheme;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.ArrayList;
import java.util.List;

public final class DecompilerPreviewRenderer {
    private static final int CLASS_PREVIEW_LINES = 7;
    private static final int METHOD_PREVIEW_LINES = 7;
    private static final int METHOD_USAGE_SURROUNDING_LINES = 2;
    private static final int USAGE_HIGHLIGHT_FILL = ImColor.rgba(145, 145, 145, 18);
    private static final int USAGE_HIGHLIGHT_BORDER = ImColor.rgba(145, 145, 145, 85);
    private static final float MAX_LINE_WIDTH = 550.F;

    private final Trinity trinity;
    private final boolean limitWidth = !ImGui.getIO().getKeyShift();
    private boolean truncated;

    public DecompilerPreviewRenderer(Trinity trinity) {
        this.trinity = trinity;
    }

    public void drawDetails(List<ColoredString> details) {
        List<PreviewSegment> line = new ArrayList<>();
        for (ColoredString detail : details) {
            String text = detail.getText().replace("\r\n", "\n").replace('\r', '\n');
            int lineStart = 0;
            int newline;
            while ((newline = text.indexOf('\n', lineStart)) != -1) {
                if (newline > lineStart) {
                    line.add(new PreviewSegment(text.substring(lineStart, newline), detail.getColor(), false));
                }
                drawDetailLine(line);
                line.clear();
                lineStart = newline + 1;
            }
            if (lineStart < text.length()) {
                line.add(new PreviewSegment(text.substring(lineStart), detail.getColor(), false));
            }
        }
        if (!line.isEmpty()) {
            drawLine(line, 0);
        }
    }

    private void drawDetailLine(List<PreviewSegment> line) {
        if (line.isEmpty()) {
            ImGui.newLine();
        } else {
            drawLine(line, 0);
        }
    }

    public void drawInputPreview(Input<?> input) {
        if (input instanceof ClassInput classInput) {
            drawDetails(ClassStructureSignatureFormatter.format(classInput));
            drawClassPreview(classInput, true);
        } else if (input instanceof MethodInput methodInput) {
            drawDetails(methodSignature(methodInput));
            drawMethodPreview(methodInput, true);
        } else if (input instanceof FieldInput fieldInput) {
            drawDetails(fieldSignature(fieldInput));
            drawFieldPreview(fieldInput, true);
        } else {
            ImGui.textUnformatted(input.toString());
        }
    }

    public void drawClassPreview(ClassInput classInput, boolean hasDetails) {
        DecompiledClass previewClass = trinity.getDecompiler().getOrDecompile(classInput);
        if (previewClass == null) {
            return;
        }

        previewClass.applyPendingOutput();
        DecompiledClass.ClassPreview preview = previewClass.getClassPreview(CLASS_PREVIEW_LINES);
        if (preview.lines().isEmpty()) {
            return;
        }

        if (hasDetails) {
            ImGui.separator();
        }
        int classIndent = preview.lines().stream()
                .mapToInt(DecompilerPreviewRenderer::getLeadingWhitespace)
                .min()
                .orElse(0);
        for (List<DecompilerLineText> line : preview.lines()) {
            drawDecompilerLine(line, classIndent);
        }
        if (preview.hasMoreLines()) {
            ImGui.textColored(CodeColorScheme.DISABLED, "...");
        }
    }

    public void drawMethodPreview(MethodInput methodInput, boolean hasDetails) {
        DecompiledClass previewClass = trinity.getDecompiler().getOrDecompile(methodInput.getOwningClass());
        if (previewClass == null) {
            return;
        }

        previewClass.applyPendingOutput();
        DecompiledClass.MethodPreview preview = previewClass.getMethodPreview(methodInput, METHOD_PREVIEW_LINES);
        if (preview.lines().isEmpty()) {
            return;
        }

        if (hasDetails) {
            ImGui.separator();
        }
        if (preview.skippedLeading()) {
            ImGui.textColored(CodeColorScheme.DISABLED, "...");
        }
        int classIndent = preview.lines().stream()
                .mapToInt(DecompilerPreviewRenderer::getLeadingWhitespace)
                .min()
                .orElse(0);
        for (List<DecompilerLineText> line : preview.lines()) {
            drawDecompilerLine(line, classIndent);
        }
        if (preview.hasMoreLines()) {
            ImGui.textColored(CodeColorScheme.DISABLED, "...");
        }
    }

    public void drawMethodUsagePreview(MethodInput methodInput, AbstractInsnNode instruction,
                                       boolean highlightOwnerClass) {
        drawMethodUsagePreview(methodInput, instruction, highlightOwnerClass, false, null);
    }

    public void drawMethodConstantUsagePreview(MethodInput methodInput, AbstractInsnNode instruction,
                                               Object constantValue) {
        drawMethodUsagePreview(methodInput, instruction, false, true, constantValue);
    }

    private void drawMethodUsagePreview(MethodInput methodInput, AbstractInsnNode instruction,
                                        boolean highlightOwnerClass, boolean highlightConstant,
                                        Object constantValue) {
        drawDetails(methodSignature(methodInput));
        DecompiledClass previewClass = trinity.getDecompiler().getOrDecompile(methodInput.getOwningClass());
        if (previewClass == null) {
            return;
        }

        previewClass.applyPendingOutput();
        DecompiledClass.MethodUsagePreview preview = previewClass.getMethodUsagePreview(
                methodInput, instruction, METHOD_USAGE_SURROUNDING_LINES,
                highlightOwnerClass, highlightConstant, constantValue);
        if (preview.signature().isEmpty()) {
            drawMethodPreview(methodInput, true);
            return;
        }

        ImGui.separator();
        int classIndent = getLeadingWhitespace(preview.signature());
        for (List<DecompilerLineText> line : preview.lines()) {
            classIndent = Math.min(classIndent, getLeadingWhitespace(line));
        }
        drawDecompilerLine(preview.signature(), classIndent);
        if (preview.skippedLeading()) {
            ImGui.textColored(CodeColorScheme.DISABLED, "...");
        }
        for (List<DecompilerLineText> line : preview.lines()) {
            drawDecompilerLine(line, classIndent, preview.usageComponent());
        }
        if (preview.hasMoreLines()) {
            ImGui.textColored(CodeColorScheme.DISABLED, "...");
        }
    }

    public void drawFieldPreview(FieldInput fieldInput, boolean hasDetails) {
        DecompiledClass previewClass = trinity.getDecompiler().getOrDecompile(fieldInput.getOwningClass());
        if (previewClass == null) {
            return;
        }

        previewClass.applyPendingOutput();
        List<DecompilerLineText> declaration = previewClass.getFieldDeclarationPreview(fieldInput);
        if (declaration.isEmpty()) {
            return;
        }
        if (hasDetails) {
            ImGui.separator();
        }
        drawDecompilerLine(declaration, Integer.MAX_VALUE);
    }

    public void drawVariablePreview(DecompiledClass decompiledClass,
                                    DecompilerComponent.VariablePreview variable, boolean hasDetails) {
        List<DecompilerLineText> declaration = decompiledClass.getVariableDeclarationPreview(
                variable.methodInput(), variable.index());
        boolean stackVariable = variable.index() >= VarExprent.STACK_BASE;
        if (declaration.isEmpty() && !stackVariable) {
            return;
        }
        if (hasDetails) {
            ImGui.separator();
        }
        if (!declaration.isEmpty()) {
            DecompilerComponent highlightedVariable = declaration.stream()
                    .map(DecompilerLineText::getComponent)
                    .filter(component -> isVariableDeclaration(component, variable))
                    .findFirst()
                    .orElse(null);
            drawDecompilerLine(declaration, Integer.MAX_VALUE, highlightedVariable);
        }
        if (stackVariable) {
            if (!declaration.isEmpty()) {
                ImGui.separator();
            }
            ImGui.textColored(CodeColorScheme.DISABLED,
                    "Variable is not defined in bytecode - it was added by the decompiler to preview stack operations");
        }
    }

    public void finish() {
        if (!truncated) {
            return;
        }
//        ImGui.separator();
//        ImGui.textColored(CodeColorScheme.DISABLED, "Preview truncated, hold SHIFT to show all");
    }

    private void drawDecompilerLine(List<DecompilerLineText> line, int leadingWhitespaceToTrim) {
        drawDecompilerLine(line, leadingWhitespaceToTrim, null);
    }

    private void drawDecompilerLine(List<DecompilerLineText> line, int leadingWhitespaceToTrim,
                                    DecompilerComponent highlightedComponent) {
        List<PreviewSegment> segments = line.stream()
                .map(text -> new PreviewSegment(text.getText(), text.getComponent().getColor(),
                        text.getComponent() == highlightedComponent))
                .toList();
        drawLine(segments, leadingWhitespaceToTrim);
    }

    private void drawLine(List<PreviewSegment> originalSegments, int leadingWhitespaceToTrim) {
        List<PreviewSegment> segments = trimLeadingWhitespace(originalSegments, leadingWhitespaceToTrim);
        if (segments.isEmpty()) {
            return;
        }

        float totalWidth = 0.F;
        for (PreviewSegment segment : segments) {
            totalWidth += ImGui.calcTextSize(segment.text()).x;
        }
        if (!limitWidth || totalWidth <= MAX_LINE_WIDTH) {
            drawSegments(segments);
            return;
        }

        truncated = true;
        float remainingWidth = Math.max(0.F, MAX_LINE_WIDTH - ImGui.calcTextSize("...").x);
        boolean rendered = false;
        for (PreviewSegment segment : segments) {
            float segmentWidth = ImGui.calcTextSize(segment.text()).x;
            String value = segment.text();
            if (segmentWidth > remainingWidth) {
                value = fitPrefix(value, remainingWidth);
            }
            if (!value.isEmpty()) {
                if (rendered) {
                    ImGui.sameLine(0.F, 0.F);
                }
                drawSegment(segment, value);
                rendered = true;
                remainingWidth -= ImGui.calcTextSize(value).x;
            }
            if (value.length() != segment.text().length() || remainingWidth <= 0.F) {
                break;
            }
        }
        if (rendered) {
            ImGui.sameLine(0.F, 0.F);
        }
        ImGui.textColored(CodeColorScheme.DISABLED, "...");
    }

    private static List<PreviewSegment> trimLeadingWhitespace(List<PreviewSegment> segments,
                                                               int leadingWhitespaceToTrim) {
        List<PreviewSegment> trimmed = new ArrayList<>(segments.size());
        int remainingWhitespace = leadingWhitespaceToTrim;
        boolean contentFound = false;
        for (PreviewSegment segment : segments) {
            String value = segment.text();
            if (!contentFound && remainingWhitespace > 0) {
                int trim = 0;
                while (trim < value.length() && trim < remainingWhitespace
                        && Character.isWhitespace(value.charAt(trim))) {
                    trim++;
                }
                value = value.substring(trim);
                remainingWhitespace -= trim;
            }
            if (value.isEmpty()) {
                continue;
            }
            contentFound = true;
            trimmed.add(new PreviewSegment(value, segment.color(), segment.highlighted()));
        }
        return trimmed;
    }

    private static void drawSegments(List<PreviewSegment> segments) {
        for (int i = 0; i < segments.size(); i++) {
            if (i != 0) {
                ImGui.sameLine(0.F, 0.F);
            }
            PreviewSegment segment = segments.get(i);
            drawSegment(segment, segment.text());
        }
    }

    private static void drawSegment(PreviewSegment segment, String text) {
        ImGui.textColored(segment.color(), text);
        if (!segment.highlighted()) {
            return;
        }

        float padding = 1.F;
        ImVec2 min = ImGui.getItemRectMin().minus(padding, padding);
        ImVec2 max = ImGui.getItemRectMax().plus(padding, padding);
        ImGui.getWindowDrawList().addRectFilled(min.x, min.y, max.x, max.y, USAGE_HIGHLIGHT_FILL);
        ImGui.getWindowDrawList().addRect(min.x, min.y, max.x, max.y, USAGE_HIGHLIGHT_BORDER);
    }

    private static String fitPrefix(String value, float maximumWidth) {
        int low = 0;
        int high = value.length();
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (ImGui.calcTextSize(value.substring(0, middle)).x <= maximumWidth) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return value.substring(0, low);
    }

    private static int getLeadingWhitespace(List<DecompilerLineText> line) {
        int whitespace = 0;
        for (DecompilerLineText text : line) {
            String value = text.getText();
            int index = 0;
            while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
                index++;
            }
            whitespace += index;
            if (index < value.length()) {
                break;
            }
        }
        return whitespace;
    }

    private static boolean isVariableDeclaration(DecompilerComponent component,
                                                 DecompilerComponent.VariablePreview variable) {
        DecompilerComponent.VariablePreview candidate = component.getPreviewVariable();
        return candidate != null && candidate.declaration()
                && candidate.index() == variable.index()
                && candidate.methodInput().getDetails().equals(variable.methodInput().getDetails());
    }

    private static List<ColoredString> methodSignature(MethodInput methodInput) {
        return ClassStructureSignatureFormatter.format(methodInput);
    }

    private static List<ColoredString> fieldSignature(FieldInput fieldInput) {
        return ClassStructureSignatureFormatter.format(fieldInput);
    }

    private record PreviewSegment(String text, int color, boolean highlighted) {
    }
}
