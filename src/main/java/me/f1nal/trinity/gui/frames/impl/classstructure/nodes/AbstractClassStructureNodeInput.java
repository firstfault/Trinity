package me.f1nal.trinity.gui.frames.impl.classstructure.nodes;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.execution.AccessFlags;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.access.SimpleAccessFlagsMaskProvider;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.frames.impl.cp.BrowserViewerNode;
import me.f1nal.trinity.gui.frames.impl.cp.RenameHandler;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.NameUtil;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractClassStructureNodeInput<I extends Input> extends ClassStructureNode {
    private final I input;

    protected AbstractClassStructureNodeInput(String icon, I input) {
        super(icon);
        this.input = input;
    }

    @Override
    protected void populatePopup(PopupItemBuilder popup) {
        popup.separator();

        getInput().populatePopup(popup);
    }

    @Override
    protected void handleLeftClick() {
        Main.getDisplayManager().openDecompilerView(this.input);
    }

    @Override
    protected final RenameHandler getRenameFunction() {
        return (newName) -> this.getInput().rename(Main.getTrinity().getRemapper(), newName);
    }

    protected abstract void appendType(ColoredStringBuilder text, String suffix);
    protected abstract void appendParameters(ColoredStringBuilder text);

    protected final void appendReturnType(ColoredStringBuilder text, String descriptor, String suffix) {
        final Type type = Type.getType(descriptor);
        final int sort = type.getSort();

        if (sort == Type.ARRAY || sort == Type.OBJECT) {
            // For referencing
            String className = (sort == Type.ARRAY ? type.getElementType() : type).getClassName();

            text.text(CodeColorScheme.CLASS_REF, NameUtil.getSimpleName(NameUtil.internalToNormal(type.getClassName())));
        } else {
            text.text(CodeColorScheme.KEYWORD, type.getClassName());
        }

        text.text(CodeColorScheme.DISABLED, suffix);
    }

    @Override
    protected BrowserViewerNode createBrowserViewerNode() {
        BrowserViewerNode node = super.createBrowserViewerNode();
        AccessFlags accessFlags = new AccessFlags(null, new SimpleAccessFlagsMaskProvider(getInput().getAccessFlagsMask()));
        node.setPrefix(safeText(prefix -> {
            appendAccessFlags(prefix, accessFlags);
            appendType(prefix, " ");
        }));
        node.setSuffix(safeText(this::appendParameters));
        return node;
    }

    private List<ColoredString> safeText(Consumer<ColoredStringBuilder> builder) {
        ColoredStringBuilder csb = ColoredStringBuilder.create();
        try {
            builder.accept(csb);
        } catch (Throwable throwable) {
            Logging.warn("Decoding type of '{}': {}", getInput().toString(), throwable);
            return ColoredStringBuilder.create().text(CodeColorScheme.NOTIFY_ERROR, "<ERROR>").get();
        }
        return csb.get();
    }

    protected void appendAccessFlags(ColoredStringBuilder text, AccessFlags accessFlags) {
        AccessFlags.Flag[] flagsArray = AccessFlags.getFlags();
        for (AccessFlags.Flag flag : flagsArray) {
            if (accessFlags.isFlag(flag) && this.getInput().isAccessFlagValid(flag)) {
                text.text(CodeColorScheme.DISABLED, flag.getName().toLowerCase() + " ");
            }
        }
    }

    public final I getInput() {
        return input;
    }
}
