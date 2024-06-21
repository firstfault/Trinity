package me.f1nal.trinity.decompiler.output;

import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.decompiler.DecompiledMethod;
import me.f1nal.trinity.decompiler.modules.decompiler.exps.VarExprent;
import me.f1nal.trinity.decompiler.output.colors.ColoredStringBuilder;
import me.f1nal.trinity.decompiler.output.number.NumberDisplayType;
import me.f1nal.trinity.decompiler.output.number.NumberDisplayTypeEnum;
import me.f1nal.trinity.decompiler.output.impl.*;
import me.f1nal.trinity.events.EventRefreshDecompilerText;
import me.f1nal.trinity.execution.*;
import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.execution.var.ImmutableVariable;
import me.f1nal.trinity.execution.var.Variable;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewCache;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewFrame;
import me.f1nal.trinity.gui.windows.impl.constant.search.ConstantSearchType;
import me.f1nal.trinity.gui.windows.impl.constant.search.ConstantSearchTypeString;
import me.f1nal.trinity.gui.windows.impl.cp.FileKind;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerComponent;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerGhostTextRenderer;
import me.f1nal.trinity.gui.windows.impl.xref.builder.IXrefBuilderProvider;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilderClassRef;
import me.f1nal.trinity.gui.windows.impl.xref.builder.XrefBuilderMemberRef;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.StringUtil;
import me.f1nal.trinity.util.SystemUtil;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
        String arrayDims = getArrayDimensions(component.getText());

        if (target != null) {
            if (!member.isKeepText()) {
                if (member.isImport()) {
                    // Imports use full internal names
                    component.setTextFunction(() -> target.getDisplayOrRealName().replace('/', '.'));
                } else {
                    component.setTextFunction(() -> target.getDisplaySimpleName() + arrayDims);
                }
            }
        }

        if (target != null && target.getInput() != null) {
            component.input = target.getInput();
            component.addInputControls(target.getInput());
        } else {
            IXrefBuilderProvider provider = xrefMap -> new XrefBuilderClassRef(xrefMap, member.getClassName());
            component.addPopupBuilder(builder -> provider.addXrefViewerMenuItem(trinity, builder));
        }

        component.memberKey = member.getClassName();
        component.setIdentifier(member, member.getClassName());
        component.setColorFunction(() -> member.isImport() || target == null ? CodeColorScheme.CLASS_REF : this.getClassKindColor(target.getKind()));
        component.setTooltip(() -> ColoredStringBuilder.create().text(CodeColorScheme.CLASS_REF, target != null ? target.getDisplayOrRealName() : member.getClassName()).get());
    }

    private int getClassKindColor(FileKind kind) {
        switch (kind) {
            case ABSTRACT -> {
                return CodeColorScheme.CLASS_REF_ABSTRACT;
            }
            case ENUM -> {
                return CodeColorScheme.CLASS_REF_ENUM;
            }
            case INTERFACES -> {
                return CodeColorScheme.CLASS_REF_INTERFACE;
            }
            case CLASSES -> {
                return CodeColorScheme.CLASS_REF;
            }
            case ANNOTATION -> {
                return CodeColorScheme.CLASS_REF_ANNOTATION;
            }
        }
        return kind.getColor();
    }

    private static String getArrayDimensions(String text) {
        StringBuilder arrayDims = new StringBuilder();

        while (text.endsWith("[]")) {
            arrayDims.append("[]");
            text = text.substring(0, text.length() - 2);
        }

        return arrayDims.toString();
    }

    @Override
    public void visitComment(CommentOutputMember comment) {
        component.addPopupBuilder(builder -> builder.menuItem("Hide comments", () -> {
            Main.getPreferences().setDecompilerHideComments(true);
            Main.getEventBus().post(new EventRefreshDecompilerText(dc -> true));
        }));

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
            builder.menuItem("Copy", () -> SystemUtil.copyToClipboard(component.getText()));
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
        String suffix = constant.getSuffix() == '\0' ? "" : String.valueOf(constant.getSuffix());
        component.setTextFunction(() -> settings.displayType.getText(number) + suffix);
        component.setColorFunction(() -> CodeColorScheme.NUMBER);
        component.setTooltip(() -> ColoredStringBuilder.create().text(component.getColor(), StringUtil.capitalizeFirstLetter(number.getClass().getSimpleName())).get());
    }

    @Override
    public void visitFieldDeclaration(FieldDeclarationOutputMember fieldDeclaration) {
        FieldInput input = trinity.getExecution().getField(new MemberDetails(fieldDeclaration));
        if (input != null) {
            component.setCustomRenderer(new DecompilerGhostTextRenderer(trinity, input));
            component.input = input;
        }
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
            component.setTextFunction(fieldInput.getDisplayName()::getName);
            component.addInputControls(fieldInput);
        } else {
            this.addXrefMemberMenuItem(component, memberDetails);
        }

        component.memberKey = memberDetails.toString();
        component.setIdentifier(field, memberDetails);
        component.setColorFunction(() -> CodeColorScheme.FIELD_REF);
        component.setTooltip(() -> ColoredStringBuilder.create()
                .text(CodeColorScheme.CLASS_REF, fieldInput != null ? fieldInput.getOwningClass().getDisplayName().getName() : field.getOwner())
                .text(CodeColorScheme.DISABLED, ".")
                .text(CodeColorScheme.FIELD_REF, fieldInput != null ? fieldInput.getDisplayName().getName() : field.getName())
                .text(CodeColorScheme.DISABLED, " ")
                .text(CodeColorScheme.DISABLED, fieldInput != null ? fieldInput.getDescriptor() : field.getFieldDescriptor())
                .get());
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
                    component.setTextFunction(methodInput.getDisplayName()::getName);
                }
            }

            component.addInputControls(methodInput);
        } else {
            this.addXrefMemberMenuItem(component, memberDetails);
        }

        component.memberKey = memberDetails.toString();
        component.setIdentifier(method, memberDetails);
        component.setColorFunction(() -> CodeColorScheme.METHOD_REF);
        component.setTooltip(() -> ColoredStringBuilder.create()
                .text(CodeColorScheme.CLASS_REF, methodInput != null ? methodInput.getOwningClass().getDisplayName().getName() : method.getOwner())
                .text(CodeColorScheme.DISABLED, ".")
                .text(CodeColorScheme.METHOD_REF, methodInput != null ? methodInput.getDisplayName().getName() : method.getName())
                .text(CodeColorScheme.DISABLED, "")
                .text(CodeColorScheme.DISABLED, methodInput != null ? methodInput.getDescriptor() : method.getMethodDescriptor())
                .get());
    }

    @Override
    public void visitKeyword(KeywordOutputMember keyword) {
        component.setColorFunction(() -> CodeColorScheme.KEYWORD);
    }

    @Override
    public void visitMethodStartEnd(MethodStartEndOutputMember methodStartEnd) {
        MethodInput input = trinity.getExecution().getMethod(new MemberDetails(methodStartEnd));
        if (input != null) {
            component.setCustomRenderer(new DecompilerGhostTextRenderer(trinity, input));
            component.input = input;
        }
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
        final String unquotedText = string.getData();

        component.addPopupBuilder(builder -> builder.menuItem("Copy", () -> SystemUtil.copyToClipboard(unquotedText))

                .menuItem("Search All Occurrences...", () -> {
                    Trinity trinity = Main.getTrinity();
                    ConstantSearchTypeString constantSearchType = new ConstantSearchTypeString(trinity);
                    constantSearchType.getSearchTerm().set(unquotedText);
                    constantSearchType.getExact().set(true);
                    List<ConstantViewCache> constantViewList = new ArrayList<>();
                    constantSearchType.populate(constantViewList);
                    Main.getWindowManager().addClosableWindow(new ConstantViewFrame(trinity, constantViewList));
                }));
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

        boolean aloadThis = varIndex == 0 && variable != null && !variable.isEditable();
        component.setIdentifier(variableMember, aloadThis ? "this" : Objects.toString(this.decompilingMethod) + varIndex);
        boolean parameter = variable != null && variable.isParameter();
        component.setColorFunction(() -> aloadThis ? CodeColorScheme.KEYWORD : parameter ? CodeColorScheme.PARAM_REF : CodeColorScheme.VAR_REF);
        component.setTooltip(() -> {
            ColoredStringBuilder text = ColoredStringBuilder.create();

            final String varType = variableMember.getType() == null ? "unknown var type!" : variableMember.getType();
            final boolean stackVariable = varIndex >= VarExprent.STACK_BASE;
            text.text(CodeColorScheme.DISABLED, (stackVariable ? "Stack Variable" : variable == null ? "#" + varIndex : variable.getName()));
            text.text(CodeColorScheme.TEXT, String.format(" (%s) ", (stackVariable ? "-" + (varIndex - VarExprent.STACK_BASE) : varIndex)));
            text.text(CodeColorScheme.CLASS_REF, varType);

            return text.get();
        });
    }

    @Override
    public void visitKind(KindOutputMember kind) {
        FileKind fileKind = kindTranslations.get(kind.getType());

        component.setColorFunction(() -> CodeColorScheme.KEYWORD);
    }

    private static final NumberDisplayType[] INTEGER_TYPES;

    private static NumberDisplayType[] getNumberDisplayTypes(NumberDisplayTypeEnum... type) {
        ArrayList<NumberDisplayType> types = new ArrayList<>();
        for (NumberDisplayTypeEnum typeEnum : type) {
            types.add(typeEnum.getInstance());
        }
        return types.toArray(new NumberDisplayType[0]);
    }

    private static final Map<KindOutputMember.KindType, FileKind> kindTranslations = new HashMap<>();

    static {
        INTEGER_TYPES = getNumberDisplayTypes(NumberDisplayTypeEnum.DECIMAL, NumberDisplayTypeEnum.HEX, NumberDisplayTypeEnum.OCTAL, NumberDisplayTypeEnum.BINARY, NumberDisplayTypeEnum.ASCII);

        kindLoop: for (KindOutputMember.KindType kindType : KindOutputMember.KindType.values()) {
            String kindName = kindType.name().substring("CLASS_".length());
            for (FileKind fileKind : FileKind.values()) {
                if (fileKind.getName().equalsIgnoreCase(kindName)) {
                    kindTranslations.put(kindType, fileKind);
                    continue kindLoop;
                }
            }
            throw new RuntimeException("No translation for kind: " + kindName);
        }
    }
}
