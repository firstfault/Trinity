package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import imgui.ImGui;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.DecompiledClass;
import me.f1nal.trinity.decompiler.modules.decompiler.exps.VarExprent;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.util.ArrayList;
import java.util.List;

public final class DecompilerPreviewRenderer {
    private static final int METHOD_PREVIEW_LINES = 7;
    private static final float MAX_LINE_WIDTH = 150.F;
    private static final String ELLIPSIS = "...";
    private static final String TRUNCATED_MESSAGE = "Preview truncated, hold SHIFT to show all";

    private final Trinity trinity;
    private final boolean limitWidth = !ImGui.getIO().getKeyShift();
    private boolean truncated;

    public DecompilerPreviewRenderer(Trinity trinity) {
        this.trinity = trinity;
    }

    public void drawDetails(List<ColoredString> details) {
        List<PreviewSegment> segments = details.stream()
                .map(detail -> new PreviewSegment(detail.getText(), detail.getColor()))
                .toList();
        drawLine(segments, 0);
    }

    public void drawInputPreview(Input<?> input) {
        if (input instanceof MethodInput methodInput) {
            drawDetails(methodSignature(methodInput));
            drawMethodPreview(methodInput, true);
        } else if (input instanceof FieldInput fieldInput) {
            drawDetails(fieldSignature(fieldInput));
            drawFieldPreview(fieldInput, true);
        } else {
            ImGui.textUnformatted(input.toString());
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
            ImGui.textColored(CodeColorScheme.DISABLED, ELLIPSIS);
        }
        int classIndent = preview.lines().stream()
                .mapToInt(DecompilerPreviewRenderer::getLeadingWhitespace)
                .min()
                .orElse(0);
        for (List<DecompilerLineText> line : preview.lines()) {
            drawDecompilerLine(line, classIndent);
        }
        if (preview.hasMoreLines()) {
            ImGui.textColored(CodeColorScheme.DISABLED, ELLIPSIS);
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
            drawDecompilerLine(declaration, Integer.MAX_VALUE);
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
        ImGui.separator();
        ImGui.textColored(CodeColorScheme.DISABLED, TRUNCATED_MESSAGE);
    }

    private void drawDecompilerLine(List<DecompilerLineText> line, int leadingWhitespaceToTrim) {
        List<PreviewSegment> segments = line.stream()
                .map(text -> new PreviewSegment(text.getText(), text.getComponent().getColor()))
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
        float remainingWidth = Math.max(0.F, MAX_LINE_WIDTH - ImGui.calcTextSize(ELLIPSIS).x);
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
                ImGui.textColored(segment.color(), value);
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
        ImGui.textColored(CodeColorScheme.DISABLED, ELLIPSIS);
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
            trimmed.add(new PreviewSegment(value, segment.color()));
        }
        return trimmed;
    }

    private static void drawSegments(List<PreviewSegment> segments) {
        for (int i = 0; i < segments.size(); i++) {
            if (i != 0) {
                ImGui.sameLine(0.F, 0.F);
            }
            PreviewSegment segment = segments.get(i);
            ImGui.textColored(segment.color(), segment.text());
        }
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

    private static List<ColoredString> methodSignature(MethodInput methodInput) {
        return List.of(
                new ColoredString(methodInput.getOwningClass().getDisplayName().getName(), CodeColorScheme.CLASS_REF),
                new ColoredString(".", CodeColorScheme.DISABLED),
                new ColoredString(methodInput.getDisplayName().getName(), CodeColorScheme.METHOD_REF),
                new ColoredString(methodInput.getDescriptor(), CodeColorScheme.DISABLED));
    }

    private static List<ColoredString> fieldSignature(FieldInput fieldInput) {
        return List.of(
                new ColoredString(fieldInput.getOwningClass().getDisplayName().getName(), CodeColorScheme.CLASS_REF),
                new ColoredString(".", CodeColorScheme.DISABLED),
                new ColoredString(fieldInput.getDisplayName().getName(), CodeColorScheme.FIELD_REF),
                new ColoredString(" " + fieldInput.getDescriptor(), CodeColorScheme.DISABLED));
    }

    private record PreviewSegment(String text, int color) {
    }
}
