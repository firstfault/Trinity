package me.f1nal.trinity.gui.viewport;

import com.google.common.eventbus.Subscribe;
import imgui.ImColor;
import imgui.ImDrawList;
import imgui.ImGui;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventClassesLoaded;
import me.f1nal.trinity.events.EventMemberModified;
import me.f1nal.trinity.events.EventPackageStructureReload;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.util.ByteUtil;
import me.f1nal.trinity.util.animation.Animation;
import me.f1nal.trinity.util.animation.Easing;
import org.objectweb.asm.commons.CodeSizeEvaluator;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ProjectNavigationBand implements IEventListener {
    private static final float BAND_HEIGHT = 14.F;
    private static final int SEGMENT_ALPHA = 82;
    private static final int HOVERED_SEGMENT_ALPHA = 118;
    private static final int BYTECODE_COLOR = ImColor.rgb(66, 126, 180);
    private static final int LIBRARY_COLOR = ImColor.rgb(155, 91, 166);
    private static final int DATA_COLOR = ImColor.rgb(82, 151, 103);
    private static final int CONSTANT_COLOR = ImColor.rgb(210, 146, 60);
    private static final int EMPTY_COLOR = ImColor.rgb(48, 48, 48);
    private static final int BORDER_COLOR = ImColor.rgba(105, 105, 105, 70);

    private final Trinity trinity;
    private List<Segment> segments = List.of();
    private final Map<String, Animation> segmentOpacity = new HashMap<>();
    private final Map<ClassInput, ClassSizeBreakdown> classSizeCache = new HashMap<>();
    private final Set<ClassInput> invalidatedClasses = new HashSet<>();
    private long totalSize;
    private volatile boolean dirty = true;
    private boolean rebuildClassCache = true;
    private Segment tooltip;

    public ProjectNavigationBand(Trinity trinity) {
        this.trinity = trinity;
        trinity.getEventManager().registerListener(this);
    }

    public void draw(float width) {
        if (dirty) {
            recalculate();
        }

        this.tooltip = null;
        drawBand(Math.max(1.F, width));
        if (this.tooltip != null) drawTooltip(tooltip.name(), tooltip.size());
    }

    private void drawBand(float width) {
        float x = ImGui.getCursorScreenPosX();
        float itemY = ImGui.getCursorScreenPosY();
        float itemHeight = ImGui.getFrameHeight();
        float y = itemY + Math.max(0.F, (itemHeight - BAND_HEIGHT) * 0.5F);
        ImDrawList drawList = ImGui.getWindowDrawList();
        ImGui.invisibleButton("###ProjectNavigationBand", width, itemHeight);
        boolean hovered = ImGui.isItemHovered();
        float mouseX = ImGui.getMousePosX();
        drawList.addRectFilled(x, y, x + width, y + BAND_HEIGHT,
                withAlpha(EMPTY_COLOR, SEGMENT_ALPHA), 2.F);

        if (totalSize == 0L) {
            if (hovered) this.tooltip = new Segment("Project contents", 0L, EMPTY_COLOR);
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
                boolean segmentHovered = hovered && mouseX >= segmentStart && mouseX < segmentEnd;
                Animation opacity = this.segmentOpacity.computeIfAbsent(segment.name(), ignored ->
                        new Animation(Easing.EASE_IN_OUT_QUAD, 120L, SEGMENT_ALPHA));
                opacity.run(segmentHovered ? HOVERED_SEGMENT_ALPHA : SEGMENT_ALPHA);
                drawList.addRectFilled(segmentStart, y, segmentEnd, y + BAND_HEIGHT,
                        withAlpha(segment.color(), Math.round(opacity.getValue())));
                if (segmentHovered) {
                    this.tooltip = segment;
                }
                segmentStart = segmentEnd;
            }
        }
        drawList.addRect(x, y, x + width, y + BAND_HEIGHT, BORDER_COLOR, 2.F);
    }

    private synchronized void recalculate() {
        Execution execution = trinity.getExecution();
        List<ClassInput> classes = new ArrayList<>(execution.getClassList());
        if (rebuildClassCache) {
            classSizeCache.clear();
            invalidatedClasses.clear();
            rebuildClassCache = false;
        } else {
            invalidatedClasses.forEach(classSizeCache::remove);
            invalidatedClasses.clear();
            classSizeCache.keySet().retainAll(new HashSet<>(classes));
        }

        long bytecode = 0L;
        long constants = 0L;
        for (ClassInput classInput : classes) {
            ClassSizeBreakdown size = classSizeCache.computeIfAbsent(classInput,
                    input -> measureClass(input.getNode()));
            bytecode += size.bytecode();
            constants += size.constants();
        }

        long libraries = 0L;
        long data = 0L;
        for (me.f1nal.trinity.execution.packages.ResourceArchiveEntry resource : execution.getResources()) {
            long size = resource.getBytes().length;
            if (isNativeLibrary(resource.getRealName())) {
                libraries += size;
            } else {
                data += size;
            }
        }

        this.segments = List.of(
                new Segment("Estimated executable bytecode", bytecode, BYTECODE_COLOR),
                new Segment("Native libraries", libraries, LIBRARY_COLOR),
                new Segment("Data resources", data, DATA_COLOR),
                new Segment("Constants", constants, CONSTANT_COLOR));
        this.totalSize = bytecode + libraries + data + constants;
        this.dirty = false;
    }

    private static boolean isNativeLibrary(String path) {
        int separator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String name = path.substring(separator + 1).toLowerCase(Locale.ROOT);
        return name.endsWith(".dll") || name.endsWith(".dylib") || name.endsWith(".so")
                || name.contains(".so.");
    }

    private static ClassSizeBreakdown measureClass(ClassNode node) {
        Map<String, Long> constants = new HashMap<>();
        long bytecode = 0L;
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
            bytecode += estimateMethodCodeSize(method);
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
        return new ClassSizeBreakdown(bytecode,
                constants.values().stream().mapToLong(Long::longValue).sum());
    }

    static long estimateExecutableBytecodeSize(ClassNode node) {
        long size = 0L;
        for (MethodNode method : node.methods) {
            size += estimateMethodCodeSize(method);
        }
        return size;
    }

    private static long estimateMethodCodeSize(MethodNode method) {
        if (method.instructions == null || method.instructions.size() == 0) return 0L;
        try {
            CodeSizeEvaluator evaluator = new CodeSizeEvaluator(null);
            method.accept(evaluator);
            return ((long) evaluator.getMinSize() + evaluator.getMaxSize() + 1L) / 2L;
        } catch (RuntimeException exception) {
            long size = 0L;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction.getOpcode() >= 0) size++;
            }
            return size;
        }
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

    private static int withAlpha(int color, int alpha) {
        return ImColor.rgba(color & 0xFF, (color >>> 8) & 0xFF,
                (color >>> 16) & 0xFF, alpha);
    }

    private static void drawTooltip(String name, long size) {
        ImGui.beginTooltip();
        ImGui.textUnformatted(name);
        ImGui.separator();
        ImGui.textUnformatted(ByteUtil.getHumanReadableByteCountSI(size));
        ImGui.endTooltip();
    }

    @Subscribe
    public synchronized void onClassesLoaded(EventClassesLoaded event) {
        rebuildClassCache = true;
        dirty = true;
    }

    @Subscribe
    public synchronized void onClassModified(EventClassModified event) {
        invalidatedClasses.add(event.getClassInput());
        dirty = true;
    }

    @Subscribe
    public synchronized void onMemberModified(EventMemberModified event) {
        invalidatedClasses.add(event.getClassInput());
        dirty = true;
    }

    @Subscribe
    public synchronized void onPackageStructureReload(EventPackageStructureReload event) {
        rebuildClassCache = true;
        dirty = true;
    }

    private record Segment(String name, long size, int color) {
    }

    private record ClassSizeBreakdown(long bytecode, long constants) {
    }
}
