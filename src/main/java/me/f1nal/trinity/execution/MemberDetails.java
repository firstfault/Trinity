package me.f1nal.trinity.execution;

import me.f1nal.trinity.decompiler.output.IMemberDetails;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import org.objectweb.asm.tree.FieldInsnNode;

import java.util.List;
import java.util.Objects;

public class MemberDetails {
    private final String owner, name, desc;

    public MemberDetails(String owner, String name, String desc) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    public MemberDetails(FieldInsnNode fieldInsnNode) {
        this(fieldInsnNode.owner, fieldInsnNode.name, fieldInsnNode.desc);
    }

    public MemberDetails(IMemberDetails details) {
        this(details.getOwner(), details.getName(), details.getDesc());
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getAll() {
        return String.format("%s%s%s", getOwner(), getName(), getDesc());
    }

    public List<ColoredString> asText(boolean method) {
        if (method) {
        return List.of(
                new ColoredString(this.getOwner(), CodeColorScheme.CLASS_REF),
                new ColoredString(".", CodeColorScheme.DISABLED),
                new ColoredString(this.getName().concat(this.desc), CodeColorScheme.METHOD_REF));
        } else {
            return List.of(
                    new ColoredString(this.getOwner(), CodeColorScheme.CLASS_REF),
                    new ColoredString(".", CodeColorScheme.DISABLED),
                    new ColoredString(this.getName() + " ", CodeColorScheme.FIELD_REF),
                    new ColoredString(this.getDesc(), CodeColorScheme.FIELD_REF));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberDetails that = (MemberDetails) o;
        return Objects.equals(owner, that.owner) && Objects.equals(name, that.name) && Objects.equals(desc, that.desc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, name, desc);
    }
}
