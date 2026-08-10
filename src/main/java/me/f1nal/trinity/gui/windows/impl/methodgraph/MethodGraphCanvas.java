package me.f1nal.trinity.gui.windows.impl.methodgraph;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiButtonFlags;
import imgui.flag.ImGuiFocusedFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiMouseCursor;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.execution.graph.MethodGraph;
import me.f1nal.trinity.execution.graph.MethodGraph.BasicBlock;
import me.f1nal.trinity.execution.graph.MethodGraph.BlockLayout;
import me.f1nal.trinity.execution.graph.MethodGraph.FlowEdge;
import me.f1nal.trinity.execution.graph.MethodGraph.FlowEdgeKind;
import me.f1nal.trinity.execution.graph.MethodGraph.InstructionLine;
import me.f1nal.trinity.execution.graph.MethodGraph.MethodKey;
import me.f1nal.trinity.execution.graph.MethodGraph.MethodNode;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerPreviewRenderer;
import me.f1nal.trinity.theme.CodeColorScheme;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Draw-list based, movable graph canvas. World coordinates never depend on docking. */
final class MethodGraphCanvas {
    private static final float MIN_ZOOM = 0.12F;
    private static final float MAX_ZOOM = 2.4F;
    private static final float ZOOM_STEP = 1.14F;
    private static final float GRID_SIZE = 32.F;
    private static final float HEADER_HEIGHT = 48.F;
    private static final float BLOCK_HEADER_HEIGHT = 21.F;
    private static final float BLOCK_BODY_PADDING_Y = 5.F;
    private static final float MINI_MAP_WIDTH = 176.F;
    private static final float MINI_MAP_HEIGHT = 112.F;
    private static final float MINI_MAP_MARGIN = 10.F;
    private static final double PREVIEW_DELAY = 0.48D;

    private final Actions actions;
    private final Map<MethodKey, Offset> manualOffsets = new HashMap<>();
    private MethodGraph graph;
    private MethodKey selected;
    private MethodKey dragging;
    private MethodKey hoveredLastFrame;
    private Set<MethodKey> searchMatches = Set.of();
    private String previousSearch = "";
    private double hoverStarted;
    private float panX;
    private float panY;
    private float zoom = 1.F;
    private float canvasX;
    private float canvasY;
    private float canvasWidth;
    private float canvasHeight;
    private boolean fitRequested;
    private boolean hovered;

    MethodGraphCanvas(Actions actions) {
        this.actions = actions;
    }

    void setGraph(MethodGraph graph) {
        boolean firstGraph = this.graph == null;
        this.graph = graph;
        this.manualOffsets.keySet().retainAll(graph.nodes().keySet());
        if (this.selected != null && !graph.nodes().containsKey(this.selected)) this.selected = null;
        if (firstGraph) this.fitRequested = true;
    }

    void setSearch(String search) {
        String normalized = search == null ? "" : search.strip().toLowerCase(Locale.ROOT);
        if (normalized.equals(previousSearch)) return;
        previousSearch = normalized;
        if (normalized.isEmpty() || graph == null) {
            searchMatches = Set.of();
            return;
        }
        Set<MethodKey> matches = new LinkedHashSet<>();
        for (MethodNode node : graph.nodes().values()) {
            String displayName = node.method() == null ? ""
                    : node.method().getDisplayName().getName().toLowerCase(Locale.ROOT);
            if (node.key().symbol().toLowerCase(Locale.ROOT).contains(normalized)
                    || displayName.contains(normalized)) {
                matches.add(node.key());
            }
        }
        searchMatches = Set.copyOf(matches);
    }

    void draw(float width, float height) {
        this.canvasWidth = width;
        this.canvasHeight = height;
        int childFlags = ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse;
        if (!ImGui.beginChild("##MethodGraphCanvas", width, height, false, childFlags)) {
            ImGui.endChild();
            return;
        }
        ImVec2 canvasPosition = ImGui.getCursorScreenPos();
        this.canvasX = canvasPosition.x;
        this.canvasY = canvasPosition.y;
        ImGui.invisibleButton("##MethodGraphCanvasInput", width, height,
                ImGuiButtonFlags.MouseButtonLeft | ImGuiButtonFlags.MouseButtonMiddle
                        | ImGuiButtonFlags.MouseButtonRight);
        this.hovered = ImGui.isItemHovered();

        if (fitRequested) fitGraph();
        updateZoom();

        ImDrawList drawList = ImGui.getWindowDrawList();
        drawList.pushClipRect(canvasX, canvasY, canvasX + width, canvasY + height, true);
        drawBackground(drawList);

        MiniMap miniMap = miniMap();
        boolean miniMapHovered = hovered && miniMap.contains(ImGui.getMousePosX(), ImGui.getMousePosY());
        Hit hit = miniMapHovered ? null : findHit(ImGui.getMousePosX(), ImGui.getMousePosY());
        handleInput(hit, miniMap, miniMapHovered);
        handleKeyboardNavigation();

        Set<MethodKey> neighborhood = selected == null ? Set.of() : neighborhood(selected);
        drawCallEdges(drawList, neighborhood);
        List<MethodNode> nodes = new ArrayList<>(graph.nodes().values());
        nodes.sort(Comparator.comparing(node -> node.key().symbol()));
        for (MethodNode node : nodes) drawMethod(drawList, node, neighborhood);
        drawMiniMap(drawList, miniMap);
        drawZoomIndicator(drawList);
        drawList.popClipRect();

        updateHoverPreview(hit);
        ImGui.endChild();
    }

    boolean isHovered() {
        return hovered;
    }

    void requestFit() {
        this.fitRequested = true;
    }

    void resetLayout() {
        manualOffsets.clear();
        fitRequested = true;
    }

    void centerRoot() {
        if (graph != null) center(graph.root());
    }

    void selectAndCenter(MethodKey key) {
        if (graph == null || !graph.nodes().containsKey(key)) return;
        selected = key;
        center(key);
    }

    private void handleInput(Hit hit, MiniMap miniMap, boolean miniMapHovered) {
        if (!hovered) {
            if (!ImGui.isMouseDown(ImGuiMouseButton.Left)) dragging = null;
            return;
        }

        if (miniMapHovered) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
            if (ImGui.isMouseDown(ImGuiMouseButton.Left)) {
                float worldX = miniMap.worldX(ImGui.getMousePosX());
                float worldY = miniMap.worldY(ImGui.getMousePosY());
                panX = canvasWidth * 0.5F - worldX * zoom;
                panY = canvasHeight * 0.5F - worldY * zoom;
            }
            dragging = null;
            return;
        }

        if (ImGui.isMouseClicked(ImGuiMouseButton.Right) && hit != null) {
            selected = hit.node().key();
            actions.showContextMenu(hit.node());
        }
        if (ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            if (hit == null) {
                selected = null;
                dragging = null;
            } else {
                selected = hit.node().key();
                dragging = hit.node().key();
                if (ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left)) {
                    actions.navigate(hit.node(), hit.instruction());
                    dragging = null;
                }
            }
        }

        if (!ImGui.isMouseDown(ImGuiMouseButton.Left)) dragging = null;
        if (dragging != null && ImGui.isMouseDragging(ImGuiMouseButton.Left)) {
            Offset offset = manualOffsets.computeIfAbsent(dragging, ignored -> new Offset());
            offset.x += ImGui.getIO().getMouseDeltaX() / zoom;
            offset.y += ImGui.getIO().getMouseDeltaY() / zoom;
            ImGui.setMouseCursor(ImGuiMouseCursor.ResizeAll);
            return;
        }

        boolean panWithLeft = hit == null && ImGui.isMouseDragging(ImGuiMouseButton.Left);
        boolean panWithMiddle = ImGui.isMouseDragging(ImGuiMouseButton.Middle);
        if (panWithLeft || panWithMiddle) {
            panX += ImGui.getIO().getMouseDeltaX();
            panY += ImGui.getIO().getMouseDeltaY();
            ImGui.setMouseCursor(ImGuiMouseCursor.ResizeAll);
        } else if (hit != null) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
        }
    }

    private void updateZoom() {
        if (!hovered) return;
        float wheel = ImGui.getIO().getMouseWheel();
        if (wheel == 0.F) return;
        float mouseX = ImGui.getMousePosX() - canvasX;
        float mouseY = ImGui.getMousePosY() - canvasY;
        float worldX = (mouseX - panX) / zoom;
        float worldY = (mouseY - panY) / zoom;
        float nextZoom = clamp(zoom * (float) Math.pow(ZOOM_STEP, wheel), MIN_ZOOM, MAX_ZOOM);
        panX = mouseX - worldX * nextZoom;
        panY = mouseY - worldY * nextZoom;
        zoom = nextZoom;
        ImGui.getIO().setMouseWheel(0.F);
    }

    private void handleKeyboardNavigation() {
        if (!ImGui.isWindowFocused(ImGuiFocusedFlags.RootAndChildWindows)
                || ImGui.isAnyItemActive()) return;
        if (ImGui.isKeyPressed(ImGuiKey.Home, false)) {
            selected = graph.root();
            centerRoot();
            return;
        }
        if (selected != null && ImGui.isKeyPressed(ImGuiKey.Enter, false)) {
            MethodNode node = graph.nodes().get(selected);
            if (node != null && node.method() != null) actions.navigate(node, null);
            return;
        }

        int horizontal = ImGui.isKeyPressed(ImGuiKey.LeftArrow, false) ? -1
                : ImGui.isKeyPressed(ImGuiKey.RightArrow, false) ? 1 : 0;
        int vertical = ImGui.isKeyPressed(ImGuiKey.UpArrow, false) ? -1
                : ImGui.isKeyPressed(ImGuiKey.DownArrow, false) ? 1 : 0;
        if (horizontal == 0 && vertical == 0) return;
        MethodNode current = selected == null ? graph.nodes().get(graph.root())
                : graph.nodes().get(selected);
        if (current == null) return;
        float currentX = worldCenterX(current);
        float currentY = worldCenterY(current);
        MethodNode best = null;
        float bestScore = Float.POSITIVE_INFINITY;
        for (MethodNode candidate : graph.nodes().values()) {
            if (candidate == current) continue;
            float dx = worldCenterX(candidate) - currentX;
            float dy = worldCenterY(candidate) - currentY;
            if (horizontal != 0 && Math.signum(dx) != horizontal) continue;
            if (vertical != 0 && Math.signum(dy) != vertical) continue;
            float primary = horizontal != 0 ? Math.abs(dx) : Math.abs(dy);
            float secondary = horizontal != 0 ? Math.abs(dy) : Math.abs(dx);
            float score = primary + secondary * 0.38F;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        if (best != null) {
            selected = best.key();
            center(best.key());
        }
    }

    private void drawBackground(ImDrawList drawList) {
        drawList.addRectFilled(canvasX, canvasY, canvasX + canvasWidth, canvasY + canvasHeight,
                CodeColorScheme.setAlpha(CodeColorScheme.BACKGROUND, 255));
        float spacing = GRID_SIZE * zoom;
        while (spacing < 14.F) spacing *= 2.F;
        int gridColor = CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 22);
        float startX = canvasX + modulo(panX, spacing);
        float startY = canvasY + modulo(panY, spacing);
        for (float x = startX; x < canvasX + canvasWidth; x += spacing) {
            drawList.addLine(x, canvasY, x, canvasY + canvasHeight, gridColor);
        }
        for (float y = startY; y < canvasY + canvasHeight; y += spacing) {
            drawList.addLine(canvasX, y, canvasX + canvasWidth, y, gridColor);
        }
    }

    private void drawCallEdges(ImDrawList drawList, Set<MethodKey> neighborhood) {
        for (MethodGraph.CallEdge edge : graph.calls()) {
            MethodNode caller = graph.nodes().get(edge.caller());
            MethodNode callee = graph.nodes().get(edge.callee());
            if (caller == null || callee == null) continue;
            boolean active = selected == null || edge.caller().equals(selected)
                    || edge.callee().equals(selected);
            int color = CodeColorScheme.setAlpha(
                    edge.dynamicDispatch() ? CodeColorScheme.KEYWORD_JUMP : CodeColorScheme.METHOD_REF,
                    active ? 150 : 30);
            Rect from = screenRect(caller);
            Rect to = screenRect(callee);
            boolean right = to.centerX() >= from.centerX();
            float x1 = right ? from.right() : from.left();
            float y1 = from.centerY();
            float x2 = right ? to.left() : to.right();
            float y2 = to.centerY();
            float bend = Math.max(45.F, Math.abs(x2 - x1) * 0.42F);
            float c1 = x1 + (right ? bend : -bend);
            float c2 = x2 - (right ? bend : -bend);
            drawList.addBezierCubic(x1, y1, c1, y1, c2, y2, x2, y2,
                    color, active ? 1.8F : 1.F);
            drawArrow(drawList, c2, y2, x2, y2, color, active ? 6.F : 5.F);
            if (active && edge.callSites() > 1 && zoom > 0.45F) {
                String label = "x" + edge.callSites();
                drawText(drawList, (x1 + x2) * 0.5F, (y1 + y2) * 0.5F - 8.F,
                        CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 210), label);
            }
        }
    }

    private void drawMethod(ImDrawList drawList, MethodNode node, Set<MethodKey> neighborhood) {
        Rect rect = screenRect(node);
        if (!rect.intersects(canvasX, canvasY, canvasX + canvasWidth, canvasY + canvasHeight)) return;
        boolean active = selected == null || neighborhood.contains(node.key());
        boolean isSelected = node.key().equals(selected);
        boolean searchMatch = searchMatches.contains(node.key());
        int alpha = active ? 255 : 78;
        int body = CodeColorScheme.setAlpha(CodeColorScheme.BACKGROUND, alpha);
        int header = CodeColorScheme.setAlpha(CodeColorScheme.WIDGET_BACKGROUND,
                active ? 245 : 74);
        int border = isSelected ? CodeColorScheme.setAlpha(CodeColorScheme.TEXT, 245)
                : searchMatch ? CodeColorScheme.setAlpha(CodeColorScheme.NOTIFY_INFORMATION, 230)
                : CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, active ? 125 : 38);

        drawList.addRectFilled(rect.left(), rect.top(), rect.right(), rect.bottom(), body);
        drawList.addRectFilled(rect.left(), rect.top(), rect.right(),
                rect.top() + HEADER_HEIGHT * zoom, header);
        if (node.root()) {
            drawList.addRectFilled(rect.left(), rect.top(), rect.left() + 3.F,
                    rect.top() + HEADER_HEIGHT * zoom,
                    CodeColorScheme.setAlpha(CodeColorScheme.METHOD_REF, active ? 230 : 75));
        }
        drawList.addRect(rect.left(), rect.top(), rect.right(), rect.bottom(), border,
                0.F, 0, isSelected ? 2.F : 1.F);
        drawMethodHeader(drawList, node, rect, active);
        if (!node.external()) drawMethodFlow(drawList, node, active);
    }

    private void drawMethodHeader(ImDrawList drawList, MethodNode node, Rect rect, boolean active) {
        if (zoom < 0.27F) return;
        int alpha = active ? 255 : 85;
        float left = rect.left() + 10.F * zoom;
        float top = rect.top() + 6.F * zoom;
        String owner = node.key().displayOwner();
        drawText(drawList, left, top, CodeColorScheme.setAlpha(CodeColorScheme.CLASS_REF, alpha), owner);
        String methodName = node.method() == null ? node.key().name()
                : node.method().getDisplayName().getName();
        drawText(drawList, left, top + 20.F * zoom,
                CodeColorScheme.setAlpha(CodeColorScheme.METHOD_REF, alpha), methodName);
        float methodWidth = textWidth(methodName);
        drawText(drawList, left + methodWidth * zoom, top + 20.F * zoom,
                CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, active ? 210 : 70),
                compactDescriptor(node.key().descriptor()));
        if (node.external()) {
            String external = "external";
            float x = rect.right() - (textWidth(external) + 9.F) * zoom;
            drawText(drawList, x, top, CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, alpha), external);
        } else if (node.root()) {
            String root = "ROOT";
            float x = rect.right() - (textWidth(root) + 9.F) * zoom;
            drawText(drawList, x, top, CodeColorScheme.setAlpha(CodeColorScheme.METHOD_REF, alpha), root);
        }
    }

    private void drawMethodFlow(ImDrawList drawList, MethodNode node, boolean active) {
        Map<Integer, BlockLayout> layouts = new HashMap<>();
        node.blocks().forEach(layout -> layouts.put(layout.blockId(), layout));
        for (FlowEdge edge : node.flow().edges()) {
            BlockLayout from = layouts.get(edge.fromBlock());
            BlockLayout to = layouts.get(edge.toBlock());
            if (from != null && to != null) drawFlowEdge(drawList, node, from, to, edge, active);
        }

        Map<Integer, BasicBlock> blocks = new HashMap<>();
        node.flow().blocks().forEach(block -> blocks.put(block.id(), block));
        for (BlockLayout layout : node.blocks()) {
            BasicBlock block = blocks.get(layout.blockId());
            if (block != null) drawBlock(drawList, node, block, layout, active);
        }
    }

    private void drawFlowEdge(ImDrawList drawList, MethodNode node,
                              BlockLayout from, BlockLayout to, FlowEdge edge, boolean active) {
        Rect source = blockScreenRect(node, from);
        Rect target = blockScreenRect(node, to);
        int color = CodeColorScheme.setAlpha(flowColor(edge.kind()), active ? 170 : 45);
        boolean backwards = target.top() <= source.top();
        float x1;
        float y1;
        float x2;
        float y2;
        float c1x;
        float c1y;
        float c2x;
        float c2y;
        if (backwards) {
            x1 = source.right();
            y1 = source.centerY();
            x2 = target.right();
            y2 = target.centerY();
            float side = Math.max(x1, x2) + 22.F * zoom;
            c1x = side;
            c1y = y1;
            c2x = side;
            c2y = y2;
        } else {
            x1 = source.centerX();
            y1 = source.bottom();
            x2 = target.centerX();
            y2 = target.top();
            float bend = Math.max(14.F * zoom, (y2 - y1) * 0.45F);
            c1x = x1;
            c1y = y1 + bend;
            c2x = x2;
            c2y = y2 - bend;
        }
        drawList.addBezierCubic(x1, y1, c1x, c1y, c2x, c2y, x2, y2, color, 1.25F);
        drawArrow(drawList, c2x, c2y, x2, y2, color, 4.5F);
        if (active && !edge.label().isBlank() && zoom > 0.58F) {
            drawText(drawList, (x1 + x2) * 0.5F + 3.F,
                    (y1 + y2) * 0.5F - 8.F,
                    CodeColorScheme.setAlpha(flowColor(edge.kind()), 225), edge.label());
        }
    }

    private void drawBlock(ImDrawList drawList, MethodNode node, BasicBlock block,
                           BlockLayout layout, boolean active) {
        Rect rect = blockScreenRect(node, layout);
        int body = CodeColorScheme.setAlpha(CodeColorScheme.WIDGET_BACKGROUND, active ? 170 : 50);
        int header = CodeColorScheme.setAlpha(CodeColorScheme.HIGHLIGHT_BACKGROUND, active ? 235 : 70);
        int border = CodeColorScheme.setAlpha(block.exceptionHandler()
                ? CodeColorScheme.NOTIFY_WARN : CodeColorScheme.DISABLED, active ? 115 : 35);
        drawList.addRectFilled(rect.left(), rect.top(), rect.right(), rect.bottom(), body);
        drawList.addRectFilled(rect.left(), rect.top(), rect.right(),
                rect.top() + BLOCK_HEADER_HEIGHT * zoom, header);
        drawList.addRect(rect.left(), rect.top(), rect.right(), rect.bottom(), border);
        if (zoom < 0.38F) return;

        String title = "B" + block.id() + (block.exceptionHandler() ? "  handler" : "");
        drawText(drawList, rect.left() + 7.F * zoom, rect.top() + 3.F * zoom,
                CodeColorScheme.setAlpha(block.exceptionHandler()
                        ? CodeColorScheme.NOTIFY_WARN : CodeColorScheme.LABEL, active ? 245 : 75), title);
        float lineY = rect.top() + (BLOCK_HEADER_HEIGHT + BLOCK_BODY_PADDING_Y) * zoom;
        int visible = Math.min(layout.visibleInstructionCount(), block.instructions().size());
        for (int index = 0; index < visible; index++) {
            InstructionLine line = block.instructions().get(index);
            String text = truncate(line.text(), 72);
            drawInstructionText(drawList, rect.left() + 7.F * zoom, lineY,
                    line, text, active);
            lineY += ImGui.getTextLineHeight() * zoom;
        }
        if (layout.hiddenInstructionCount() > 0) {
            drawText(drawList, rect.left() + 7.F * zoom, lineY,
                    CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, active ? 210 : 65),
                    "... " + layout.hiddenInstructionCount() + " more");
        } else if (block.instructions().isEmpty()) {
            drawText(drawList, rect.left() + 7.F * zoom, lineY,
                    CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, active ? 210 : 65),
                    "no bytecode");
        }
    }

    private Hit findHit(float mouseX, float mouseY) {
        if (graph == null) return null;
        List<MethodNode> nodes = new ArrayList<>(graph.nodes().values());
        nodes.sort(Comparator.comparing((MethodNode node) -> node.key().equals(selected)).reversed()
                .thenComparing(node -> node.key().symbol()));
        for (MethodNode node : nodes) {
            Rect rect = screenRect(node);
            if (!rect.contains(mouseX, mouseY)) continue;
            AbstractInsnNode instruction = instructionAt(node, mouseX, mouseY);
            return new Hit(node, instruction);
        }
        return null;
    }

    private AbstractInsnNode instructionAt(MethodNode node, float mouseX, float mouseY) {
        if (node.flow() == null || zoom < 0.38F) return null;
        Map<Integer, BasicBlock> blocks = new HashMap<>();
        node.flow().blocks().forEach(block -> blocks.put(block.id(), block));
        for (BlockLayout layout : node.blocks()) {
            Rect rect = blockScreenRect(node, layout);
            if (!rect.contains(mouseX, mouseY)) continue;
            float lineTop = rect.top() + (BLOCK_HEADER_HEIGHT + BLOCK_BODY_PADDING_Y) * zoom;
            int index = (int) ((mouseY - lineTop) / (ImGui.getTextLineHeight() * zoom));
            BasicBlock block = blocks.get(layout.blockId());
            if (block != null && index >= 0
                    && index < Math.min(layout.visibleInstructionCount(), block.instructions().size())) {
                return block.instructions().get(index).instruction();
            }
        }
        return null;
    }

    private void updateHoverPreview(Hit hit) {
        MethodKey hoveredKey = hit == null ? null : hit.node().key();
        if (hoveredKey == null || !hoveredKey.equals(hoveredLastFrame)) {
            hoveredLastFrame = hoveredKey;
            hoverStarted = ImGui.getTime();
            return;
        }
        if (dragging != null || hit.node().method() == null
                || ImGui.getTime() - hoverStarted < PREVIEW_DELAY) return;
        ImGui.beginTooltip();
        DecompilerPreviewRenderer preview = new DecompilerPreviewRenderer(
                hit.node().method().getOwningClass().getExecution().getTrinity());
        if (hit.instruction() == null) {
            preview.drawInputPreview(hit.node().method());
        } else {
            preview.drawMethodUsagePreview(hit.node().method(), hit.instruction(), false);
        }
        preview.finish();
        ImGui.endTooltip();
    }

    private Set<MethodKey> neighborhood(MethodKey key) {
        Set<MethodKey> result = new HashSet<>();
        result.add(key);
        for (MethodGraph.CallEdge edge : graph.calls()) {
            if (edge.caller().equals(key)) result.add(edge.callee());
            if (edge.callee().equals(key)) result.add(edge.caller());
        }
        return result;
    }

    private void fitGraph() {
        fitRequested = false;
        MethodGraph.Bounds bounds = currentBounds();
        float width = Math.max(1.F, bounds.width());
        float height = Math.max(1.F, bounds.height());
        zoom = clamp(Math.min((canvasWidth - 90.F) / width,
                (canvasHeight - 90.F) / height), MIN_ZOOM, 1.F);
        float centerX = (bounds.minX() + bounds.maxX()) * 0.5F;
        float centerY = (bounds.minY() + bounds.maxY()) * 0.5F;
        panX = canvasWidth * 0.5F - centerX * zoom;
        panY = canvasHeight * 0.5F - centerY * zoom;
    }

    private void center(MethodKey key) {
        MethodNode node = graph == null ? null : graph.nodes().get(key);
        if (node == null) return;
        Offset offset = offset(key);
        float centerX = node.x() + offset.x + node.width() * 0.5F;
        float centerY = node.y() + offset.y + node.height() * 0.5F;
        panX = canvasWidth * 0.5F - centerX * zoom;
        panY = canvasHeight * 0.5F - centerY * zoom;
    }

    private MethodGraph.Bounds currentBounds() {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (MethodNode node : graph.nodes().values()) {
            Offset offset = offset(node.key());
            minX = Math.min(minX, node.x() + offset.x);
            minY = Math.min(minY, node.y() + offset.y);
            maxX = Math.max(maxX, node.x() + offset.x + node.width());
            maxY = Math.max(maxY, node.y() + offset.y + node.height());
        }
        if (!Float.isFinite(minX)) return new MethodGraph.Bounds(0.F, 0.F, 1.F, 1.F);
        return new MethodGraph.Bounds(minX, minY, maxX, maxY);
    }

    private MiniMap miniMap() {
        MethodGraph.Bounds bounds = currentBounds();
        float left = canvasX + canvasWidth - MINI_MAP_WIDTH - MINI_MAP_MARGIN;
        float top = canvasY + canvasHeight - MINI_MAP_HEIGHT - MINI_MAP_MARGIN;
        float padding = 7.F;
        float scale = Math.min((MINI_MAP_WIDTH - padding * 2.F) / Math.max(1.F, bounds.width()),
                (MINI_MAP_HEIGHT - padding * 2.F) / Math.max(1.F, bounds.height()));
        float graphLeft = left + (MINI_MAP_WIDTH - bounds.width() * scale) * 0.5F;
        float graphTop = top + (MINI_MAP_HEIGHT - bounds.height() * scale) * 0.5F;
        return new MiniMap(left, top, left + MINI_MAP_WIDTH, top + MINI_MAP_HEIGHT,
                bounds, scale, graphLeft, graphTop);
    }

    private void drawMiniMap(ImDrawList drawList, MiniMap miniMap) {
        drawList.addRectFilled(miniMap.left(), miniMap.top(), miniMap.right(), miniMap.bottom(),
                CodeColorScheme.setAlpha(CodeColorScheme.BACKGROUND, 232));
        drawList.addRect(miniMap.left(), miniMap.top(), miniMap.right(), miniMap.bottom(),
                CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 105));
        for (MethodNode node : graph.nodes().values()) {
            Offset offset = offset(node.key());
            float left = miniMap.screenX(node.x() + offset.x);
            float top = miniMap.screenY(node.y() + offset.y);
            float right = miniMap.screenX(node.x() + offset.x + node.width());
            float bottom = miniMap.screenY(node.y() + offset.y + node.height());
            int color = node.key().equals(selected)
                    ? CodeColorScheme.setAlpha(CodeColorScheme.TEXT, 225)
                    : node.root() ? CodeColorScheme.setAlpha(CodeColorScheme.METHOD_REF, 210)
                    : CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 115);
            drawList.addRectFilled(left, top, Math.max(left + 1.F, right),
                    Math.max(top + 1.F, bottom), color);
        }
        float viewMinX = (-panX) / zoom;
        float viewMinY = (-panY) / zoom;
        float viewMaxX = (canvasWidth - panX) / zoom;
        float viewMaxY = (canvasHeight - panY) / zoom;
        drawList.addRect(miniMap.screenX(viewMinX), miniMap.screenY(viewMinY),
                miniMap.screenX(viewMaxX), miniMap.screenY(viewMaxY),
                CodeColorScheme.setAlpha(CodeColorScheme.TEXT, 185));
    }

    private void drawZoomIndicator(ImDrawList drawList) {
        String label = Math.round(zoom * 100.F) + "%";
        float x = canvasX + 9.F;
        float y = canvasY + canvasHeight - ImGui.getTextLineHeight() - 9.F;
        drawText(drawList, x, y, CodeColorScheme.setAlpha(CodeColorScheme.DISABLED, 180), label);
    }

    private Rect screenRect(MethodNode node) {
        Offset offset = offset(node.key());
        return new Rect(screenX(node.x() + offset.x), screenY(node.y() + offset.y),
                screenX(node.x() + offset.x + node.width()),
                screenY(node.y() + offset.y + node.height()));
    }

    private float worldCenterX(MethodNode node) {
        Offset offset = offset(node.key());
        return node.x() + offset.x + node.width() * 0.5F;
    }

    private float worldCenterY(MethodNode node) {
        Offset offset = offset(node.key());
        return node.y() + offset.y + node.height() * 0.5F;
    }

    private Rect blockScreenRect(MethodNode node, BlockLayout block) {
        Offset offset = offset(node.key());
        float worldX = node.x() + offset.x + block.x();
        float worldY = node.y() + offset.y + block.y();
        return new Rect(screenX(worldX), screenY(worldY),
                screenX(worldX + block.width()), screenY(worldY + block.height()));
    }

    private float screenX(float worldX) {
        return canvasX + panX + worldX * zoom;
    }

    private float screenY(float worldY) {
        return canvasY + panY + worldY * zoom;
    }

    private Offset offset(MethodKey key) {
        return manualOffsets.getOrDefault(key, Offset.ZERO);
    }

    private void drawText(ImDrawList drawList, float x, float y, int color, String text) {
        if (text == null || text.isEmpty()) return;
        int size = Math.max(7, Math.round(ImGui.getFontSize() * zoom));
        drawList.addText(ImGui.getFont(), size, x, y, color, text);
    }

    private void drawInstructionText(ImDrawList drawList, float x, float y,
                                     InstructionLine line, String text, boolean active) {
        int separator = text.indexOf(' ');
        String opcode = separator == -1 ? text : text.substring(0, separator);
        drawText(drawList, x, y,
                CodeColorScheme.setAlpha(instructionColor(line), active ? 245 : 74), opcode);
        if (separator == -1) return;
        float operandX = x + textWidth(opcode) * zoom;
        drawText(drawList, operandX, y,
                CodeColorScheme.setAlpha(CodeColorScheme.TEXT, active ? 210 : 63),
                text.substring(separator));
    }

    private static void drawArrow(ImDrawList drawList, float fromX, float fromY,
                                  float toX, float toY, int color, float size) {
        float dx = toX - fromX;
        float dy = toY - fromY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001F) return;
        dx /= length;
        dy /= length;
        float px = -dy;
        float py = dx;
        float baseX = toX - dx * size;
        float baseY = toY - dy * size;
        drawList.addTriangleFilled(toX, toY,
                baseX + px * size * 0.55F, baseY + py * size * 0.55F,
                baseX - px * size * 0.55F, baseY - py * size * 0.55F, color);
    }

    private static int flowColor(FlowEdgeKind kind) {
        return switch (kind) {
            case TRUE_BRANCH -> CodeColorScheme.NOTIFY_SUCCESS;
            case FALSE_BRANCH -> CodeColorScheme.NOTIFY_ERROR;
            case SWITCH -> CodeColorScheme.KEYWORD_JUMP;
            case EXCEPTION -> CodeColorScheme.NOTIFY_WARN;
            case JUMP -> CodeColorScheme.KEYWORD_JUMP;
            case FALLTHROUGH -> CodeColorScheme.DISABLED;
        };
    }

    private static int instructionColor(InstructionLine line) {
        return switch (line.kind()) {
            case CALL -> CodeColorScheme.KEYWORD_CALL;
            case BRANCH -> CodeColorScheme.KEYWORD_JUMP;
            case DATA -> CodeColorScheme.KEYWORD_DATA;
            case TERMINAL -> CodeColorScheme.KEYWORD;
            case NORMAL -> CodeColorScheme.TEXT;
        };
    }

    private static String compactDescriptor(String descriptor) {
        if (descriptor.length() <= 42) return descriptor;
        return descriptor.substring(0, 39) + "...";
    }

    private static String truncate(String text, int maximum) {
        return text.length() <= maximum ? text : text.substring(0, maximum - 3) + "...";
    }

    private static float textWidth(String text) {
        return ImGui.calcTextSize(text).x;
    }

    private static float modulo(float value, float modulus) {
        float result = value % modulus;
        return result < 0.F ? result + modulus : result;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    interface Actions {
        void navigate(MethodNode node, AbstractInsnNode instruction);

        void showContextMenu(MethodNode node);
    }

    private record Hit(MethodNode node, AbstractInsnNode instruction) {
    }

    private record Rect(float left, float top, float right, float bottom) {
        float centerX() {
            return (left + right) * 0.5F;
        }

        float centerY() {
            return (top + bottom) * 0.5F;
        }

        boolean contains(float x, float y) {
            return x >= left && x <= right && y >= top && y <= bottom;
        }

        boolean intersects(float left, float top, float right, float bottom) {
            return this.right >= left && this.left <= right
                    && this.bottom >= top && this.top <= bottom;
        }
    }

    private static final class Offset {
        private static final Offset ZERO = new Offset();
        private float x;
        private float y;
    }

    private record MiniMap(float left, float top, float right, float bottom,
                           MethodGraph.Bounds bounds, float scale,
                           float graphLeft, float graphTop) {
        boolean contains(float x, float y) {
            return x >= left && x <= right && y >= top && y <= bottom;
        }

        float screenX(float worldX) {
            return graphLeft + (worldX - bounds.minX()) * scale;
        }

        float screenY(float worldY) {
            return graphTop + (worldY - bounds.minY()) * scale;
        }

        float worldX(float screenX) {
            return bounds.minX() + (screenX - graphLeft) / scale;
        }

        float worldY(float screenY) {
            return bounds.minY() + (screenY - graphTop) / scale;
        }
    }
}
