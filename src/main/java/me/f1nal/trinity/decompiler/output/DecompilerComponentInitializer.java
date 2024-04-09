package me.f1nal.trinity.decompiler.output;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.DecompiledMethod;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.decompiler.output.number.NumberDisplayType;
import me.f1nal.trinity.decompiler.output.number.NumberDisplayTypeEnum;
import me.f1nal.trinity.decompiler.output.impl.*;
import me.f1nal.trinity.execution.*;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.execution.var.ImmutableVariable;
import me.f1nal.trinity.execution.var.Variable;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewCache;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewFrame;
import me.f1nal.trinity.gui.windows.impl.constant.search.ConstantSearchType;
import me.f1nal.trinity.gui.windows.impl.constant.search.ConstantSearchTypeString;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerComponent;
import me.f1nal.trinity.gui.windows.impl.xref.builder.IXrefBuilderProvider;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilderClassRef;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilderMemberRef;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.StringUtil;
import me.f1nal.trinity.util.SystemUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DecompilerComponentInitializer implements OutputMemberVisitor {
    private final Trinity trinity;
    /**
     * Component we are building.
     */
    private final DecompilerComponent component;
    /**
     * Original text that was linked to this component by the decompiler.
     */
    private final String originalText;
    /**
     * The class that this component corresponds to.
     */
    private final ClassInput decompilingClass;
    /**
     * The method that this component corresponds to.
     */
    private final DecompiledMethod decompilingMethod;

    public DecompilerComponentInitializer(Trinity trinity, DecompilerComponent component, String originalText, ClassInput decompilingClass, DecompiledMethod decompilingMethod) {
        this.trinity = trinity;
        this.component = component;
        this.originalText = originalText;
        this.decompilingClass = decompilingClass;
        this.decompilingMethod = decompilingMethod;
    }

    @Override
    public void visitBytecodeMarker(BytecodeMarkerOutputMember bytecodeMarker) {
        throw new IllegalStateException("Bytecode markers are not implemented yet");
    }

    @Override
    public void visitClass(ClassOutputMember member) {
        ClassTarget target = trinity.getExecution().getClassTarget(member.getClassName());

        if (target != null) {
            if (member.isImport()) {
                // Imports use full internal names
                component.setTextFunction(() -> target.getDisplayOrRealName().replace('/', '.'));
            } else {
                component.setTextFunction(target::getDisplaySimpleName);
            }
        }

        if (target != null && target.getInput() != null) {
            component.addInputControls(target.getInput());
        } else {
            IXrefBuilderProvider provider = xrefMap -> new XrefBuilderClassRef(xrefMap, member.getClassName());
            component.addPopupBuilder(builder -> provider.addXrefViewerMenuItem(trinity, builder));
        }

        component.setIdentifier(member, member.getClassName());
        component.setColorFunction(() -> CodeColorScheme.CLASS_REF);
        component.setTooltip(() -> ColoredStringBuilder.create().text(CodeColorScheme.CLASS_REF, target != null ? target.getDisplayOrRealName() : member.getClassName()).get());
    }

    @Override
    public void visitComment(CommentOutputMember comment) {
        component.addPopupBuilder(builder -> {
            builder.menuItem("Hide comments", () -> {
                Main.getPreferences().setDecompilerHideComments(true);
                Main.getDisplayManager().getArchiveEntryViewerFacade().resetDecompilerComponents();
            });
        });

        component.setTextFunction(() -> Main.getPreferences().isDecompilerHideComments() ? "" : this.originalText);
        component.setColorFunction(() -> CodeColorScheme.DISABLED);
    }

    @Override
    public void visitNumber(NumberOutputMember constant) {
        final Number number = constant.getNumber();
        var settings = new Object() {
            NumberDisplayType displayType = Main.getPreferences().getDefaultNumberDisplayType().getInstance();
        };

        component.addPopupBuilder(builder -> {
            builder.menuItem("Copy", () -> {
                SystemUtil.copyToClipboard(component.getText());
            });
            builder.separator();
            for (NumberDisplayType displayType : INTEGER_TYPES) {
                builder.menuItem(String.format("%s (%s)", displayType.getLabel(), displayType.getText(number)), () -> {
                    settings.displayType = displayType;
                    component.refreshWindow();
                });
            }
            builder.separator();
            builder.menuItem("Search All Occurrences...", () -> {
                Trinity trinity = Main.getTrinity();
                ConstantSearchType constantSearchType = settings.displayType.getConstantSearchType(trinity, number);
                List<ConstantViewCache> constantViewList = new ArrayList<>();
                constantSearchType.populate(constantViewList);
                Main.getWindowManager().addClosableWindow(new ConstantViewFrame(trinity, constantViewList));
            });
        });

        component.setIdentifier(constant, number);
        component.setTextFunction(() -> settings.displayType.getText(number));
        component.setColorFunction(() -> CodeColorScheme.NUMBER);
        component.setTooltip(() -> ColoredStringBuilder.create().text(component.getColor(), StringUtil.capitalizeFirstLetter(number.getClass().getSimpleName())).get());
    }

    @Override
    public void visitFieldDeclaration(FieldDeclarationOutputMember fieldDeclaration) {
    }

    private void addXrefMemberMenuItem(DecompilerComponent component, MemberDetails memberDetails) {
        IXrefBuilderProvider provider = xrefMap -> new XrefBuilderMemberRef(xrefMap, memberDetails);
        component.addPopupBuilder(builder -> provider.addXrefViewerMenuItem(trinity, builder));
    }

    @Override
    public void visitField(FieldOutputMember field) {
        MemberDetails memberDetails = new MemberDetails(field);
        @Nullable FieldInput fieldInput = trinity.getExecution().getField(memberDetails);

        if (fieldInput != null) {
            component.setTextFunction(fieldInput::getDisplayName);
            component.addInputControls(fieldInput);
        } else {
            this.addXrefMemberMenuItem(component, memberDetails);
        }

        component.setIdentifier(field, memberDetails);
        component.setColorFunction(() -> CodeColorScheme.FIELD_REF);
    }

    @Override
    public void visitMethod(MethodOutputMember method) {
        MemberDetails memberDetails = new MemberDetails(method);
        @Nullable MethodInput methodInput = trinity.getExecution().getMethod(memberDetails);

        if (methodInput != null) {
            // super()/this() calls aren't named
            if (!this.originalText.equals("super") && !this.originalText.equals("this")) {
                if (methodInput.isInit()) {
                    // <init> translates to the class name
                    component.setTextFunction(methodInput.getOwningClass()::getDisplaySimpleName);
                } else if (!methodInput.isClinit()) {
                    // not <clinit>, regular method so this is ok
                    component.setTextFunction(methodInput::getDisplayName);
                }
            }

            component.addInputControls(methodInput);
        } else {
            this.addXrefMemberMenuItem(component, memberDetails);
        }

        component.setIdentifier(method, memberDetails);
        component.setColorFunction(() -> CodeColorScheme.METHOD_REF);
    }

    @Override
    public void visitKeyword(KeywordOutputMember keyword) {
        component.setColorFunction(() -> CodeColorScheme.KEYWORD);
    }

    @Override
    public void visitMethodStartEnd(MethodStartEndOutputMember methodStartEnd) {
    }

    @Override
    public void visitPackage(PackageOutputMember pkgMember) {
        if (pkgMember.isParent()) {
            component.setColorFunction(() -> CodeColorScheme.KEYWORD);
            component.setTextFunction(() -> decompilingClass.getClassTarget().getPackage().getParent() == null ? "" : "package ");
            return;
        }

        component.setColorFunction(() -> CodeColorScheme.PACKAGE);
        component.setTooltip(() -> {
            Package pkg = decompilingClass.getClassTarget().getPackage();

            return ColoredStringBuilder.create().
                    text(CodeColorScheme.PACKAGE, pkg.getPrettyPath()).newline().
                    fmt(CodeColorScheme.TEXT, CodeColorScheme.STRING, "{} files, {} directories", pkg.getEntries().size(), pkg.getPackages().size()).
                    get();
        });
        component.setTextFunction(() -> {
            if (decompilingClass.getClassTarget().getPackage().getParent() == null) {
                return "";
            }
            return String.format("%s;\n\n", decompilingClass.getClassTarget().getPackage().getPrettyPath());
        });
    }

    @Override
    public void visitString(StringOutputMember string) {
        // FIXME: Not the actual original string, it is escaped!
        final String unquotedText = this.originalText.substring(1, this.originalText.length() - 1);

        component.addPopupBuilder(builder -> {
            builder.menuItem("Copy", () -> SystemUtil.copyToClipboard(unquotedText))

                    .menuItem("Search All Occurrences...", () -> {
                        Trinity trinity = Main.getTrinity();
                        ConstantSearchTypeString constantSearchType = new ConstantSearchTypeString(trinity);
                        constantSearchType.getSearchTerm().set(unquotedText);
                        constantSearchType.getExact().set(true);
                        List<ConstantViewCache> constantViewList = new ArrayList<>();
                        constantSearchType.populate(constantViewList);
                        Main.getWindowManager().addClosableWindow(new ConstantViewFrame(trinity, constantViewList));
                    });
        });
        component.setIdentifier(string, originalText.hashCode());
        component.setColorFunction(() -> CodeColorScheme.STRING);
    }

    @Override
    public void visitVariable(VariableOutputMember variableMember) {
        final int varIndex = variableMember.getVar();
        final Variable variable = decompilingMethod == null ? null : decompilingMethod.getMethodInput().getVariableTable().getVariable(varIndex);

        if (variable != null) {
            component.setRenameHandler(variable.getRenameHandler());
            component.setTextFunction(variable::getName);
        }

        component.setIdentifier(variableMember, variable instanceof ImmutableVariable ? "this" : Objects.toString(this.decompilingMethod) + varIndex);
        component.setColorFunction(() -> CodeColorScheme.VAR_REF);
        component.setTooltip(() -> ColoredStringBuilder.create()
                .text(CodeColorScheme.DISABLED, "#" + varIndex + " ")
                .text(CodeColorScheme.CLASS_REF, variableMember.getType() == null ? "unknown var type!" : variableMember.getType()).get());
    }

    private static final NumberDisplayType[] INTEGER_TYPES;

    private static NumberDisplayType[] getNumberDisplayTypes(NumberDisplayTypeEnum... type) {
        ArrayList<NumberDisplayType> types = new ArrayList<>();
        for (NumberDisplayTypeEnum typeEnum : type) {
            types.add(typeEnum.getInstance());
        }
        return types.toArray(new NumberDisplayType[0]);
    }

    static {
        INTEGER_TYPES = getNumberDisplayTypes(NumberDisplayTypeEnum.DECIMAL, NumberDisplayTypeEnum.HEX, NumberDisplayTypeEnum.OCTAL, NumberDisplayTypeEnum.BINARY, NumberDisplayTypeEnum.ASCII);
    }
}
