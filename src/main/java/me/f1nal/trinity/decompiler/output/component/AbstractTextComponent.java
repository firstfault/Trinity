package me.f1nal.trinity.decompiler.output.component;

import me.f1nal.trinity.decompiler.output.effect.TextComponentEffect;
import me.f1nal.trinity.util.NameUtil;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTextComponent {
    private final List<TextComponentEffect> effectList = new ArrayList<>();
    private List<AbstractInsnNode> linkedInstructions;
    private final String text;
    private int id;

    public AbstractTextComponent(String text) {
        this.text = this instanceof RawTextComponent || this instanceof CommentComponent ? text : NameUtil.cleanNewlines(text);
    }

    public void setLinkedInstructions(List<AbstractInsnNode> linkedInstructions) {
        this.linkedInstructions = linkedInstructions;
    }

    public List<AbstractInsnNode> getLinkedInstructions() {
        return linkedInstructions;
    }

    protected void addEffect(TextComponentEffect effect) {
        this.effectList.add(effect);
    }

    public List<TextComponentEffect> getEffectList() {
        return effectList;
    }

    public String getText() {
        return text;
    }

    public int getTextColor() {
        return -1;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public abstract boolean handleItemHover();

    public void handleAfterDrawing() {
    }
}
