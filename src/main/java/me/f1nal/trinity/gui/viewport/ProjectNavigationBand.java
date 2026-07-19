package me.f1nal.trinity.gui.viewport;

import com.google.common.eventbus.Subscribe;
import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.datapool.DataPool;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.events.EventPackageStructureReload;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.util.ByteUtil;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ProjectNavigationBand implements IEventListener {
    public static final float HEIGHT = 14.F;
    private static final float BAND_HEIGHT = 14.F;
    private static final float LABEL_PADDING = 3.F;
    private static final int LABEL_COLOR = ImColor.rgba(245, 245, 245, 210);
    private static final int BYTECODE_COLOR = ImColor.rgb(66, 126, 180);
    private static final int LIBRARY_COLOR = ImColor.rgb(155, 91, 166);
    private static final int DATA_COLOR = ImColor.rgb(82, 151, 103);
    private static final int CONSTANT_COLOR = ImColor.rgb(210, 146, 60);
    private static final int EMPTY_COLOR = ImColor.rgb(48, 48, 48);
    private static final int BORDER_COLOR = ImColor.rgba(105, 105, 105, 180);

    private final Trinity trinity;
    private List<Segment> segments = List.of();
    private long totalSize;
    private volatile boolean dirty = true;
    private Segment tooltip;

    public ProjectNavigationBand(Trinity trinity) {
        this.trinity = trinity;
        trinity.getEventManager().registerListener(this);
    }

    public void draw() {
        if (dirty) {
            recalculate();
        }

        this.tooltip = null;

        ImGuiViewport viewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(viewport.getWorkPosX(), viewport.getWorkPosY());
        ImGui.setNextWindowSize(viewport.getWorkSizeX(), HEIGHT);
        ImGui.setNextWindowViewport(viewport.getID());
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.F, 0.F);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.F);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.F);
        int flags = ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse
                | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoScrollbar
                | ImGuiWindowFlags.NoScrollWithMouse | ImGuiWindowFlags.NoSavedSettings
                | ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoFocusOnAppearing
                | ImGuiWindowFlags.NoNavFocus;
        if (ImGui.begin("Project Navigation Band###TrinityProjectNavigationBand", flags)) {
            drawBand();
        }
        ImGui.end();
        ImGui.popStyleVar(3);
        if (this.tooltip != null) drawTooltip(tooltip.name(), tooltip.size());
    }

    private void drawBand() {
        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        float width = Math.max(1.F, ImGui.getContentRegionAvailX());
        ImDrawList drawList = ImGui.getWindowDrawList();
        drawList.addRectFilled(x, y, x + width, y + BAND_HEIGHT, EMPTY_COLOR);

        if (totalSize == 0L) {
            ImGui.setCursorScreenPos(x, y);
            ImGui.invisibleButton("###EmptyProjectNavigationBand", width, BAND_HEIGHT);
            if (ImGui.isItemHovered()) {
                drawTooltip("Project contents", 0L);
            }
        } else {
            int visibleSegments = 0;
            for (Segment segment : segments) {
                if (segment.size() != 0L) {
                    visibleSegments++;
                }
            }
            float proportionalWidth = Math.max(0.F, width - visibleSegments);
            int drawnSegments = 0;
            float segmentStart = x;
            for (Segment segment : segments) {
                if (segment.size() == 0L) {
                    continue;
                }
                drawnSegments++;
                float segmentWidth = 1.F + proportionalWidth * ((float) segment.size() / (float) totalSize);
                float segmentEnd = drawnSegments == visibleSegments ? x + width : segmentStart + segmentWidth;
                ImGui.setCursorScreenPos(segmentStart, y);
                ImGui.invisibleButton("###ProjectNavigationBand" + segment.id(), segmentEnd - segmentStart, BAND_HEIGHT);
                boolean hovered = ImGui.isItemHovered();
                drawList.addRectFilled(segmentStart, y, segmentEnd, y + BAND_HEIGHT,
                        hovered ? brighten(segment.color()) : segment.color());
                drawSegmentLabel(drawList, segment, segmentStart, segmentEnd, y);
                if (hovered) {
                    this.tooltip = segment;
                }
                segmentStart = segmentEnd;
            }
        }
        drawList.addRect(x, y, x + width, y + BAND_HEIGHT, BORDER_COLOR);
    }

    private static void drawSegmentLabel(ImDrawList drawList, Segment segment, float startX, float endX, float y) {
        FontManager fontManager = Main.getDisplayManager().getFontManager();
        ImFont font = fontManager.getNavigationBandFont();
        if (font == null) {
            return;
        }
        float fontSize = fontManager.getNavigationBandFontSize();
        ImVec2 textSize = font.calcTextSizeA(fontSize, Float.MAX_VALUE, 0.F, segment.label());
        if (endX - startX < textSize.x + LABEL_PADDING * 2.F) {
            return;
        }
        float textY = y + Math.max(0.F, (BAND_HEIGHT - textSize.y) * 0.5F);
        drawList.pushClipRect(startX, y, endX, y + BAND_HEIGHT, true);
        drawList.addText(font, Math.round(fontSize), startX + LABEL_PADDING, textY, LABEL_COLOR, segment.label());
        drawList.popClipRect();
    }

    private void recalculate() {
        Execution execution = trinity.getExecution();
        long serializedClasses = 0L;
        long constants = 0L;
        for (ClassInput classInput : new ArrayList<>(execution.getClassList())) {
            long classSize;
            try {
                classSize = DataPool.writeClassNode(classInput.getNode()).length;
            } catch (RuntimeException exception) {
                classSize = Math.max(0, classInput.getClassTarget().getSizeInBytes());
            }
            long classConstants = Math.min(classSize, constantPayloadSize(classInput.getNode()));
            serializedClasses += classSize;
            constants += classConstants;
        }

        long libraries = 0L;
        long data = 0L;
        for (Map.Entry<String, byte[]> resource : new HashMap<>(execution.getResourceMap()).entrySet()) {
            long size = resource.getValue() == null ? 0L : resource.getValue().length;
            if (isNativeLibrary(resource.getKey())) {
                libraries += size;
            } else {
                data += size;
            }
        }

        long bytecode = Math.max(0L, serializedClasses - constants);
        this.segments = List.of(
                new Segment("Bytecode", "BYTECODE", "Executable bytecode", bytecode, BYTECODE_COLOR),
                new Segment("Libraries", "LIBRARY", "Native libraries", libraries, LIBRARY_COLOR),
                new Segment("Data", "DATA", "Data resources", data, DATA_COLOR),
                new Segment("Constants", "CONSTANT", "Constants", constants, CONSTANT_COLOR));
        this.totalSize = bytecode + libraries + data + constants;
        this.dirty = false;
    }

    private static boolean isNativeLibrary(String path) {
        int separator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String name = path.substring(separator + 1).toLowerCase(Locale.ROOT);
        return name.endsWith(".dll") || name.endsWith(".dylib") || name.endsWith(".so")
                || name.contains(".so.");
    }

    private static long constantPayloadSize(ClassNode node) {
        Map<String, Long> constants = new HashMap<>();
        addAnnotations(constants, node.visibleAnnotations);
        addAnnotations(constants, node.invisibleAnnotations);
        addAnnotations(constants, node.visibleTypeAnnotations);
        addAnnotations(constants, node.invisibleTypeAnnotations);

        if (node.recordComponents != null) {
            for (RecordComponentNode component : node.recordComponents) {
                addAnnotations(constants, component.visibleAnnotations);
                addAnnotations(constants, component.invisibleAnnotations);
                addAnnotations(constants, component.visibleTypeAnnotations);
                addAnnotations(constants, component.invisibleTypeAnnotations);
            }
        }
        for (FieldNode field : node.fields) {
            addConstant(constants, field.value);
            addAnnotations(constants, field.visibleAnnotations);
            addAnnotations(constants, field.invisibleAnnotations);
            addAnnotations(constants, field.visibleTypeAnnotations);
            addAnnotations(constants, field.invisibleTypeAnnotations);
        }
        for (MethodNode method : node.methods) {
            addConstant(constants, method.annotationDefault);
            addAnnotations(constants, method.visibleAnnotations);
            addAnnotations(constants, method.invisibleAnnotations);
            addAnnotations(constants, method.visibleTypeAnnotations);
            addAnnotations(constants, method.invisibleTypeAnnotations);
            addParameterAnnotations(constants, method.visibleParameterAnnotations);
            addParameterAnnotations(constants, method.invisibleParameterAnnotations);
            addAnnotations(constants, method.visibleLocalVariableAnnotations);
            addAnnotations(constants, method.invisibleLocalVariableAnnotations);
            if (method.tryCatchBlocks != null) {
                for (TryCatchBlockNode block : method.tryCatchBlocks) {
                    addAnnotations(constants, block.visibleTypeAnnotations);
                    addAnnotations(constants, block.invisibleTypeAnnotations);
                }
            }
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof LdcInsnNode ldc) {
                    addConstant(constants, ldc.cst);
                } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                    addConstant(constants, dynamic.bsm);
                    for (Object argument : dynamic.bsmArgs) {
                        addConstant(constants, argument);
                    }
                }
            }
        }
        return constants.values().stream().mapToLong(Long::longValue).sum();
    }

    private static void addParameterAnnotations(Map<String, Long> constants, List<AnnotationNode>[] annotations) {
        if (annotations == null) {
            return;
        }
        for (List<AnnotationNode> parameter : annotations) {
            addAnnotations(constants, parameter);
        }
    }

    private static void addAnnotations(Map<String, Long> constants,
                                       List<? extends AnnotationNode> annotations) {
        if (annotations == null) {
            return;
        }
        annotations.forEach(annotation -> addAnnotation(constants, annotation));
    }

    private static void addAnnotation(Map<String, Long> constants, AnnotationNode annotation) {
        if (annotation == null || annotation.values == null) {
            return;
        }
        for (int i = 1; i < annotation.values.size(); i += 2) {
            addConstant(constants, annotation.values.get(i));
        }
    }

    private static void addConstant(Map<String, Long> constants, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof AnnotationNode annotation) {
            addAnnotation(constants, annotation);
        } else if (value instanceof String string) {
            add(constants, "S:" + string, string.getBytes(StandardCharsets.UTF_8).length);
        } else if (value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Boolean || value instanceof Character || value instanceof Float) {
            add(constants, value.getClass().getName() + ':' + value, 4L);
        } else if (value instanceof Long || value instanceof Double) {
            add(constants, value.getClass().getName() + ':' + value, 8L);
        } else if (value instanceof Type type) {
            String descriptor = type.getDescriptor();
            add(constants, "T:" + descriptor, descriptor.getBytes(StandardCharsets.UTF_8).length);
        } else if (value instanceof Handle handle) {
            String text = handle.getOwner() + '.' + handle.getName() + handle.getDesc();
            add(constants, "H:" + handle.getTag() + ':' + text,
                    text.getBytes(StandardCharsets.UTF_8).length);
        } else if (value instanceof ConstantDynamic dynamic) {
            String identity = dynamic.getName() + dynamic.getDescriptor();
            add(constants, "D:" + identity, identity.getBytes(StandardCharsets.UTF_8).length);
            addConstant(constants, dynamic.getBootstrapMethod());
            for (int i = 0; i < dynamic.getBootstrapMethodArgumentCount(); i++) {
                addConstant(constants, dynamic.getBootstrapMethodArgument(i));
            }
        } else if (value instanceof List<?> list) {
            list.forEach(item -> addConstant(constants, item));
        } else if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                addConstant(constants, Array.get(value, i));
            }
        }
    }

    private static void add(Map<String, Long> constants, String key, long size) {
        constants.putIfAbsent(key, Math.max(0L, size));
    }

    private static int brighten(int color) {
        int red = Math.min(255, (color & 0xFF) + 28);
        int green = Math.min(255, ((color >>> 8) & 0xFF) + 28);
        int blue = Math.min(255, ((color >>> 16) & 0xFF) + 28);
        int alpha = (color >>> 24) & 0xFF;
        return ImColor.rgba(red, green, blue, alpha);
    }

    private static void drawTooltip(String name, long size) {
        ImGui.beginTooltip();
        ImGui.textUnformatted(name);
        ImGui.separator();
        ImGui.textUnformatted(ByteUtil.getHumanReadableByteCountSI(size));
        ImGui.endTooltip();
    }

    @Subscribe
    public void onClassesLoaded(EventClassesLoaded event) {
        dirty = true;
    }

    @Subscribe
    public void onClassModified(EventClassModified event) {
        dirty = true;
    }

    @Subscribe
    public void onMemberModified(EventMemberModified event) {
        dirty = true;
    }

    @Subscribe
    public void onPackageStructureReload(EventPackageStructureReload event) {
        dirty = true;
    }

    private record Segment(String id, String label, String name, long size, int color) {
    }
}
