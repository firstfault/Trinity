package me.f1nal.trinity.adapter;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.application.AnalysisService;
import me.f1nal.trinity.application.ApplicationException;
import me.f1nal.trinity.application.BrowseService;
import me.f1nal.trinity.application.Page;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.MemberDetails;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.pattern.InstructionPatternCompiler;
import me.f1nal.trinity.execution.pattern.InstructionPatternMatch;
import me.f1nal.trinity.execution.pattern.InstructionPatternMatcher;
import me.f1nal.trinity.execution.xref.AbstractXref;
import me.f1nal.trinity.execution.xref.MemberXref;
import me.f1nal.trinity.execution.xref.where.XrefWhereMethodInsn;
import me.f1nal.trinity.gui.windows.impl.assembler.AssemblerClipboardCodec;
import me.f1nal.trinity.util.InstructionUtil;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.util.Printer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Desktop adapter for deterministic analysis queries. */
final class LiveAnalysisService implements AnalysisService {
    private static final int MAX_PAGE_SIZE = 500;

    private final LiveApplicationState state;

    LiveAnalysisService(LiveApplicationState state) {
        this.state = state;
    }

    @Override
    public Page<XrefResult> findClassReferences(ClassReferenceQuery query) {
        int offset = offset(query.offset());
        int limit = limit(query.limit());
        return state.read(true, project -> {
            LiveBrowseService.requireClass(project, query.internalName());
            List<XrefResult> results = project.getExecution().getXrefMap()
                    .queryClassReferences(query.internalName()).stream()
                    .map(this::xrefResult)
                    .sorted(xrefOrder())
                    .toList();
            return Page.slice(results, offset, limit);
        });
    }

    @Override
    public Page<XrefResult> findMemberReferences(MemberReferenceQuery query) {
        int offset = offset(query.offset());
        int limit = limit(query.limit());
        return state.read(true, project -> {
            BrowseService.MemberId id = query.member();
            if (id.descriptor().startsWith("(")) LiveBrowseService.requireMethod(project, id);
            else LiveBrowseService.requireField(project, id);
            List<XrefResult> results = project.getExecution().getXrefMap()
                    .queryMemberReferences(id.owner(), id.name(), id.descriptor()).stream()
                    .map(this::xrefResult)
                    .sorted(xrefOrder())
                    .toList();
            return Page.slice(results, offset, limit);
        });
    }

    @Override
    public Page<ConstantResult> searchConstants(ConstantQuery query) {
        int offset = offset(query.offset());
        int limit = limit(query.limit());
        String type = query.type() == null || query.type().isBlank()
                ? "all" : query.type().toLowerCase(Locale.ROOT);
        return state.read(true, project -> {
            List<ConstantResult> results = new ArrayList<>();
            for (ClassInput owner : project.getExecution().getClassList()) {
                for (MethodInput method : owner.getMethodMap().values()) {
                    AbstractInsnNode[] instructions = method.getInstructions().toArray();
                    for (int index = 0; index < instructions.length; index++) {
                        Object value = constant(instructions[index]);
                        if (value == null || !typeMatches(type, value)
                                || !valueMatches(query, value)) continue;
                        String formatted = AssemblerClipboardCodec.formatInstruction(
                                instructions[index], label -> method.getLabelTable()
                                        .getLabel(label.getLabel()).getName());
                        results.add(new ConstantResult(constantType(value), jsonValue(value),
                                LiveBrowseService.memberId(method), index, formatted));
                    }
                }
            }
            results.sort(Comparator.comparing((ConstantResult result) ->
                            LiveBrowseService.identity(result.method()))
                    .thenComparingInt(ConstantResult::instructionIndex));
            return Page.slice(results, offset, limit);
        });
    }

    @Override
    public PatternValidation validatePattern(PatternQuery query) {
        var compilation = InstructionPatternCompiler.compile(query.pattern(), query.includeMetadata());
        return new PatternValidation(compilation.pattern() != null,
                compilation.pattern() == null ? 0 : compilation.pattern().instructionPatternCount(),
                compilation.diagnostics().stream().map(diagnostic -> new PatternDiagnostic(
                        diagnostic.line(), diagnostic.column(), diagnostic.severity().name(),
                        diagnostic.message())).toList());
    }

    @Override
    public Page<PatternMatch> searchPattern(PatternSearch query) {
        int offset = offset(query.offset());
        int limit = limit(query.limit());
        var compilation = InstructionPatternCompiler.compile(query.pattern(), query.includeMetadata());
        if (compilation.pattern() == null) {
            String message = compilation.diagnostics().stream()
                    .map(diagnostic -> "line " + diagnostic.line() + ": " + diagnostic.message())
                    .findFirst().orElse("Invalid pattern");
            throw new ApplicationException(ApplicationException.Code.INVALID_INPUT, message);
        }
        return state.read(true, project -> {
            List<PatternMatch> matches = new ArrayList<>();
            for (ClassInput owner : project.getExecution().getClassList()) {
                if (query.owner() != null && !query.owner().isBlank()
                        && !owner.getRealName().equals(query.owner())) continue;
                for (MethodInput method : owner.getMethodMap().values()) {
                    AbstractInsnNode[] all = method.getInstructions().toArray();
                    for (InstructionPatternMatch match : InstructionPatternMatcher.findAll(
                            method, compilation.pattern())) {
                        int start = indexOf(all, match.firstInstruction());
                        int end = indexOf(all, match.lastInstruction());
                        matches.add(new PatternMatch(LiveBrowseService.memberId(method),
                                start, end, match.formattedInstructions()));
                    }
                }
            }
            matches.sort(Comparator.comparing((PatternMatch result) ->
                            LiveBrowseService.identity(result.method()))
                    .thenComparingInt(PatternMatch::startInstructionIndex));
            return Page.slice(matches, offset, limit);
        });
    }

    @Override
    public InvocationDetails getInvocation(InvocationQuery query) {
        return state.read(true, project -> {
            MethodInput caller = LiveBrowseService.requireMethod(project, query.caller());
            AbstractInsnNode[] instructions = caller.getInstructions().toArray();
            if (query.instructionIndex() < 0 || query.instructionIndex() >= instructions.length) {
                throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                        "instructionIndex is outside the method instruction list");
            }
            AbstractInsnNode instruction = instructions[query.instructionIndex()];
            String owner;
            String name;
            String descriptor;
            boolean interfaceOwner;
            String functionKind;
            List<Object> bootstrapArguments = new ArrayList<>();
            if (instruction instanceof MethodInsnNode method) {
                owner = method.owner;
                name = method.name;
                descriptor = method.desc;
                interfaceOwner = method.itf;
                functionKind = "method";
            } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                owner = dynamic.bsm.getOwner();
                name = dynamic.name;
                descriptor = dynamic.desc;
                interfaceOwner = dynamic.bsm.isInterface();
                functionKind = "invokedynamic";
                for (Object argument : dynamic.bsmArgs) bootstrapArguments.add(jsonValue(argument));
            } else {
                throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                        "Instruction " + query.instructionIndex() + " is not an invocation");
            }
            MethodInput target = project.getExecution().getMethod(new MemberDetails(owner, name, descriptor));
            Type returnType = Type.getReturnType(descriptor);
            return new InvocationDetails(query.caller(), query.instructionIndex(),
                    opcode(instruction), functionKind, owner, name, descriptor, interfaceOwner,
                    target != null, target == null ? null : LiveBrowseService.memberId(target),
                    returnType.getClassName(),
                    List.of(Type.getArgumentTypes(descriptor)).stream().map(Type::getClassName).toList(),
                    bootstrapArguments, state.revision());
        });
    }

    private XrefResult xrefResult(AbstractXref xref) {
        Input<?> where = xref.getWhere().getInput();
        MethodInput caller = where instanceof MethodInput method ? method : null;
        Integer index = null;
        if (caller != null) {
            AbstractInsnNode instruction = xref instanceof MemberXref member ? member.getInstruction()
                    : xref.getWhere() instanceof XrefWhereMethodInsn methodInsn
                    ? methodInsn.getInsnNode() : null;
            if (instruction != null) index = indexOf(caller.getInstructions().toArray(), instruction);
        }
        return new XrefResult(xref.getKind().getName(), xref.getAccessText(), xref.getInvocation(),
                caller == null ? null : LiveBrowseService.memberId(caller), index,
                xref.getWhere().getText());
    }

    private static Comparator<XrefResult> xrefOrder() {
        return Comparator.comparing((XrefResult result) -> result.caller() == null
                        ? "" : LiveBrowseService.identity(result.caller()))
                .thenComparing(result -> result.instructionIndex() == null ? -1 : result.instructionIndex())
                .thenComparing(XrefResult::kind);
    }

    private static Object constant(AbstractInsnNode instruction) {
        if (instruction instanceof LdcInsnNode ldc) return ldc.cst;
        if (instruction instanceof IntInsnNode integer) return integer.operand;
        if (instruction instanceof IincInsnNode increment) return increment.incr;
        int opcode = instruction.getOpcode();
        if ((opcode >= org.objectweb.asm.Opcodes.ICONST_M1 && opcode <= org.objectweb.asm.Opcodes.DCONST_1)
                && opcode != org.objectweb.asm.Opcodes.BIPUSH && opcode != org.objectweb.asm.Opcodes.SIPUSH) {
            try {
                return InstructionUtil.decodeConstLoad(opcode);
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean typeMatches(String expected, Object value) {
        if (expected.equals("all") || expected.equals("any")) return true;
        return constantType(value).equals(expected)
                || expected.equals("number") && value instanceof Number;
    }

    private static boolean valueMatches(ConstantQuery query, Object value) {
        String expected = query.value();
        if (expected == null || expected.isEmpty()) return true;
        String actual = value instanceof Type type ? type.getDescriptor() : String.valueOf(value);
        if (!query.caseSensitive()) {
            expected = expected.toLowerCase(Locale.ROOT);
            actual = actual.toLowerCase(Locale.ROOT);
        }
        return query.exact() ? actual.equals(expected) : actual.contains(expected);
    }

    private static String constantType(Object value) {
        if (value instanceof String) return "string";
        if (value instanceof Integer || value instanceof Byte || value instanceof Short) return "integer";
        if (value instanceof Long) return "long";
        if (value instanceof Float) return "float";
        if (value instanceof Double) return "double";
        if (value instanceof Type) return "type";
        if (value instanceof Handle) return "handle";
        return value.getClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

    private static Object jsonValue(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Type type) return type.getDescriptor();
        if (value instanceof Handle handle) {
            return handle.getOwner() + "." + handle.getName() + handle.getDesc();
        }
        return Objects.toString(value);
    }

    private static String opcode(AbstractInsnNode instruction) {
        int opcode = instruction.getOpcode();
        return opcode < 0 ? instruction.getClass().getSimpleName()
                : Printer.OPCODES[opcode].toLowerCase(Locale.ROOT);
    }

    private static int indexOf(AbstractInsnNode[] instructions, AbstractInsnNode target) {
        for (int index = 0; index < instructions.length; index++) {
            if (instructions[index] == target) return index;
        }
        return -1;
    }

    private static int offset(int value) {
        if (value < 0) throw new ApplicationException(ApplicationException.Code.INVALID_INPUT,
                "offset must not be negative");
        return value;
    }

    private static int limit(int value) {
        return value <= 0 ? 100 : Math.min(value, MAX_PAGE_SIZE);
    }
}
