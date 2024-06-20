package me.f1nal.trinity.theme;

import imgui.ImColor;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static me.f1nal.trinity.theme.ThemeColorCategory.*;

public final class CodeColorScheme {
    /////////////////////////
    // General Colors
    /////////////////////////
    @LabeledColor(category = GENERAL, label = "Error")
    public static int NOTIFY_ERROR = getRgb(new Color(164, 60, 60));
    @LabeledColor(category = GENERAL, label = "Warning")
    public static int NOTIFY_WARN = getRgb(new Color(189, 141, 55));
    @LabeledColor(category = GENERAL, label = "Information")
    public static int NOTIFY_INFORMATION = getRgb(new Color(58, 113, 152));
    @LabeledColor(category = GENERAL, label = "Success")
    public static int NOTIFY_SUCCESS = getRgb(new Color(96, 152, 58));

    /////////////////////////
    // Frame Colors
    /////////////////////////
    @LabeledColor(category = FRAME, label = "Highlight Background")
    public static int HIGHLIGHT_BACKGROUND = getRgb(new Color(16, 16, 16));
    @LabeledColor(category = FRAME, label = "Background")
    @ImGuiColor(targets = {
            ImGuiCol.WindowBg
    })
    public static int BACKGROUND = getRgb(new Color(31, 31, 31));
    @LabeledColor(category = FRAME, label = "Popup Background")
    @ImGuiColor(targets = {
            ImGuiCol.PopupBg
    })
    public static int POPUP_BACKGROUND = getRgb(new Color(30, 30, 30));
    @LabeledColor(category = FRAME, label = "Widget Background")
    @ImGuiColor(targets = {
            ImGuiCol.FrameBg
    })
    public static int WIDGET_BACKGROUND = getRgb(new Color(54, 54, 54, 138));

    /////////////////////////
    // File Kind
    /////////////////////////
    @LabeledColor(category = FILE_KIND, label = "Interface")
    public static int FILE_INTERFACE = getRgb(new Color(99, 162, 81));
    @LabeledColor(category = FILE_KIND, label = "Abstract")
    public static int FILE_ABSTRACT = getRgb(new Color(119, 90, 176));
    @LabeledColor(category = FILE_KIND, label = "Enum")
    public static int FILE_ENUM = getRgb(new Color(192, 133, 73));
    @LabeledColor(category = FILE_KIND, label = "Resource")
    public static int FILE_RESOURCE = getRgb(new Color(157, 57, 84));

    /////////////////////////
    // Decompiler Colors
    /////////////////////////
    @LabeledColor(category = CODE_EDITOR, label = "Method Reference")
    public static int METHOD_REF = getRgb(new Color(103, 165, 204));
    @LabeledColor(category = CODE_EDITOR, label = "Field Reference")
    public static int FIELD_REF = getRgb(new Color(137, 68, 164));
    @LabeledColor(category = CODE_EDITOR, label = "Class Reference")
    public static int CLASS_REF = getRgb(new Color(64, 87, 133, 215));
    @LabeledColor(category = CODE_EDITOR, label = "Class Reference (Interface)")
    public static int CLASS_REF_INTERFACE = FILE_INTERFACE;
    @LabeledColor(category = CODE_EDITOR, label = "Class Reference (Abstract)")
    public static int CLASS_REF_ABSTRACT = FILE_ABSTRACT;
    @LabeledColor(category = CODE_EDITOR, label = "Class Reference (Enum)")
    public static int CLASS_REF_ENUM = FILE_ENUM;
    @LabeledColor(category = CODE_EDITOR, label = "Variable Reference")
    public static int VAR_REF = getRgb(new Color(61, 134, 141, 215));
    @LabeledColor(category = CODE_EDITOR, label = "Parameter Reference")
    public static int PARAM_REF = getRgb(new Color(120, 211, 253, 215));
    @LabeledColor(category = CODE_EDITOR, label = "Archive")
    public static int ARCHIVE_REF = getRgb(new Color(117, 78, 134));
    @LabeledColor(category = CODE_EDITOR, label = "Keyword")
    public static int KEYWORD = getRgb(new Color(125, 142, 159));
    @LabeledColor(category = CODE_EDITOR, label = "Label")
    public static int LABEL = getRgb(new Color(157, 157, 157));
    @LabeledColor(category = CODE_EDITOR, label = "Number")
    public static int NUMBER = getRgb(new Color(117, 77, 196));
    @LabeledColor(category = CODE_EDITOR, label = "Disabled")
    @ImGuiColor(targets = {
            ImGuiCol.TextDisabled
    })
    public static int DISABLED = getRgb(new Color(125, 125, 125));
    @LabeledColor(category = CODE_EDITOR, label = "Text")
    @ImGuiColor(targets = {
            ImGuiCol.Text
    })
    public static int TEXT = getRgb(new Color(185, 185, 185));
    @LabeledColor(category = CODE_EDITOR, label = "Package")
    public static int PACKAGE = getRgb(new Color(157, 101, 71));
    @LabeledColor(category = CODE_EDITOR, label = "String")
    public static int STRING = getRgb(new Color(211, 211, 211));
    @LabeledColor(category = CODE_EDITOR, label = "Line Number")
    public static int LINE_NUMBER = getRgb(new Color(66, 110, 66));
    @LabeledColor(category = CODE_EDITOR, label = "Cursor")
    public static int CURSOR = getRgb(new Color(175, 175, 175));
    @LabeledColor(category = CODE_EDITOR, label = "Cursor Selection")
    public static int CURSOR_SELECTION = getRgb(new Color(83, 117, 189));

    /////////////////////////
    // Assembler Instruction Types
    /////////////////////////
    @LabeledColor(category = ASSEMBLER, label = "Data")
    public static int KEYWORD_DATA = getRgb(new Color(65, 151, 213));
    @LabeledColor(category = ASSEMBLER, label = "Jump")
    public static int KEYWORD_JUMP = getRgb(new Color(215, 138, 61));
    @LabeledColor(category = ASSEMBLER, label = "Call")
    public static int KEYWORD_CALL = getRgb(new Color(121, 192, 70));

    /////////////////////////
    // Xref Kind
    /////////////////////////
    @LabeledColor(category = XREF_KIND, label = "Inherit")
    public static int XREF_INHERIT = getRgb(new Color(96, 140, 65));
    @LabeledColor(category = XREF_KIND, label = "Return")
    public static int XREF_RETURN = getRgb(new Color(123, 134, 73));
    @LabeledColor(category = XREF_KIND, label = "Parameter")
    public static int XREF_PARAMETER = getRgb(new Color(134, 73, 90));
    @LabeledColor(category = XREF_KIND, label = "Literal")
    public static int XREF_LITERAL = getRgb(new Color(111, 180, 178));
    public static int XREF_TYPE = CLASS_REF;
    public static int XREF_INVOKE = METHOD_REF;
    public static int XREF_FIELD = FIELD_REF;
    @LabeledColor(category = XREF_KIND, label = "Exception")
    public static int XREF_EXCEPTION = getRgb(new Color(192, 125, 70));
    @LabeledColor(category = XREF_KIND, label = "Annotation")
    public static int XREF_ANNOTATION = getRgb(new Color(136, 66, 145));

    public static float[] toRgba(int in) {
        Color clr = toColor(in);
        return new float[] {
                clr.getRed() / 255.F,
                clr.getGreen() / 255.F,
                clr.getBlue() / 255.F,
                clr.getAlpha() / 255.F,
        };
    }

    public static Color toColor(int in) {
        return new Color(
                ((in >> 0) & 0xFF),
                ((in >> 8) & 0xFF),
                ((in >> 16) & 0xFF),
                ((in >> 24) & 0xFF));
//        a << 24 | b << 16 | g << 8 | r;
    }

    public static int getRgb(Color color) {
        return getRgb(color, color.getAlpha());
    }

    public static int getRgb(Color color, int alpha) {
        return ImColor.rgba(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static int setAlpha(int color, int alpha) {
        return getRgb(toColor(color), alpha);
    }

    private static final List<CodeColor> codeColors = new ArrayList<>();
    private static boolean colorListenersEnabled;

    static {
        Arrays.stream(CodeColorScheme.class.getDeclaredFields()).filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == Integer.TYPE).forEach(field -> {
            final @Nullable LabeledColor labeledColor = field.getDeclaredAnnotation(LabeledColor.class);
            if (labeledColor == null) {
                return;
            }
            field.setAccessible(true);
            CodeColor codeColor = new CodeColor(labeledColor.label(), labeledColor.category(), field);
            codeColors.add(codeColor);

            final @Nullable ImGuiColor imGuiColor = field.getDeclaredAnnotation(ImGuiColor.class);
            if (imGuiColor != null) {
                codeColor.getListeners().add(() -> {
                    if (!colorListenersEnabled) {
                        return;
                    }
                    final int color = codeColor.getColor();

                    for (int target : imGuiColor.targets()) {
                        ImGui.getStyle().setColor(target, color);
                    }
                });
            }
        });

        Arrays.stream(CodeColorScheme.class.getDeclaredMethods()).forEach(method -> {
            final @Nullable ColorChangeListener colorChangeListener = method.getDeclaredAnnotation(ColorChangeListener.class);
            if (colorChangeListener == null) {
                return;
            }
            method.setAccessible(true);
            final CodeColor codeColor = Objects.requireNonNull(getCodeColor(colorChangeListener.category(), colorChangeListener.label()), "Color change listener for unregistered color");
            codeColor.getListeners().add(() -> {
                if (!colorListenersEnabled) {
                    return;
                }
                try {
                    method.invoke(null);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException("Invoking color change listener", e);
                }
            });
        });
    }

    public static void enableColorListeners() {
        colorListenersEnabled = true;
        for (CodeColor codeColor : codeColors) {
            for (Runnable listener : codeColor.getListeners()) {
                listener.run();
            }
        }
    }

    public static CodeColor getCodeColor(ThemeColorCategory category, String label) {
        for (CodeColor codeColor : codeColors) {
            if (codeColor.getCategory() == category && codeColor.getLabel().equals(label)) {
                return codeColor;
            }
        }
        return null;
    }

    public static List<CodeColor> getCodeColors() {
        return codeColors;
    }

    public static int getRgb(float[] rgba) {
        return ImColor.rgba(rgba[0], rgba[1], rgba[2], rgba[3]);
    }
}
