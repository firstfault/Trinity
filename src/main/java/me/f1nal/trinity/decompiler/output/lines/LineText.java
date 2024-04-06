package me.f1nal.trinity.decompiler.output.lines;

import imgui.ImGui;
import imgui.flag.ImGuiCol;

import java.util.function.Supplier;

public abstract class LineText {
    public abstract void render();

    public final static class LineTextComponent extends LineText {
        private final String text;
        private final Supplier<Integer> color;

        public LineTextComponent(String text, Supplier<Integer> color) {
            this.text = text;
            this.color = color;
        }

        @Override
        public void render() {
            ImGui.pushStyleColor(ImGuiCol.Text, this.color.get());
            ImGui.text(text);
            ImGui.popStyleColor();
        }
    }

    public final static class LineTextNewline extends LineText {
        @Override
        public void render() {
            ImGui.newLine();
        }
    }

    public final static class LineTextSameLine extends LineText {
        @Override
        public void render() {
            ImGui.sameLine(0.F, 0.F);
        }
    }
}
