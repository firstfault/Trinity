package me.f1nal.trinity.execution.dex;

import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder;
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import com.android.tools.smali.smali.InvalidToken;
import com.android.tools.smali.smali.smaliFlexLexer;
import com.android.tools.smali.smali.smaliParser;
import com.android.tools.smali.smali.smaliTreeWalker;
import me.f1nal.trinity.application.BrowseService;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.TokenSource;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.TreeNodeStream;

import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** Parses smali and reconstructs one complete DEX file around an edited class. */
public final class DexEditor {
    private DexEditor() {
    }

    public static Candidate replaceClass(DexClassEntry target, String smali) {
        if (smali == null || smali.isBlank()) {
            return Candidate.invalid(target, List.of(new Diagnostic(
                    0, 0, "error", "smali must not be blank")));
        }
        Assembly assembly = assemble(smali, target.getFile().getDexFile().getOpcodes());
        if (!assembly.diagnostics().isEmpty()) {
            return Candidate.invalid(target, assembly.diagnostics());
        }
        String actualName = DexDescriptors.internalName(assembly.classDef().getType());
        if (!target.getInternalName().equals(actualName)) {
            return Candidate.invalid(target, List.of(new Diagnostic(0, 0, "error",
                    String.format("Replacement declares %s but target is %s",
                            actualName, target.getInternalName()))));
        }
        return rebuild(target, assembly.classDef());
    }

    public static Candidate replaceMethod(DexClassEntry owner, BrowseService.MemberId method,
                                          String methodSmali) {
        if (methodSmali == null || methodSmali.isBlank()) {
            return Candidate.invalid(owner, List.of(new Diagnostic(
                    0, 0, "error", "smali must not be blank")));
        }
        Method originalMethod = owner.findMethod(method.name(), method.descriptor());
        if (originalMethod == null) {
            return Candidate.invalid(owner, List.of(new Diagnostic(0, 0, "error",
                    String.format("DEX method not found: %s.%s%s", owner.getInternalName(),
                            method.name(), method.descriptor()))));
        }

        String classSmali = owner.disassemble();
        String originalSmali = DexDisassembler.disassembleMethod(
                owner.getFile(), owner.getClassDef(), originalMethod);
        int methodOffset = classSmali.indexOf(originalSmali);
        if (methodOffset < 0) {
            return Candidate.invalid(owner, List.of(new Diagnostic(0, 0, "error",
                    "Unable to locate the method in canonical class smali")));
        }
        String replacement = String.format("%s%n", methodSmali.strip());
        String editedClass = String.format("%s%s%s", classSmali.substring(0, methodOffset),
                replacement, classSmali.substring(methodOffset + originalSmali.length()));
        Assembly assembly = assemble(editedClass, owner.getFile().getDexFile().getOpcodes());
        if (!assembly.diagnostics().isEmpty()) {
            return Candidate.invalid(owner, assembly.diagnostics());
        }
        if (findMethod(assembly.classDef(), method) == null) {
            return Candidate.invalid(owner, List.of(new Diagnostic(0, 0, "error",
                    "Replacement must preserve the target method name and descriptor")));
        }
        return rebuild(owner, assembly.classDef());
    }

    private static Candidate rebuild(DexClassEntry target, ClassDef replacement) {
        try {
            DexFileUnit originalFile = target.getFile();
            DexPool output = new DexPool(originalFile.getDexFile().getOpcodes());
            for (ClassDef classDef : originalFile.getDexFile().getClasses()) {
                output.internClass(classDef.getType().equals(target.getClassDef().getType())
                        ? replacement : classDef);
            }
            MemoryDataStore data = new MemoryDataStore();
            output.writeTo(data);
            byte[] bytes = data.getData();
            DexBackedDexFile verified = new DexBackedDexFile(
                    originalFile.getDexFile().getOpcodes(), bytes);
            if (verified.getClasses().size() != originalFile.getDexFile().getClasses().size()) {
                return Candidate.invalid(target, List.of(new Diagnostic(0, 0, "error",
                        "Rebuilt DEX changed the number of classes")));
            }
            return new Candidate(true, originalFile.getName(), target.getInternalName(), bytes,
                    fingerprint(bytes), List.of());
        } catch (Exception exception) {
            return Candidate.invalid(target, List.of(new Diagnostic(0, 0, "error",
                    exception.getMessage() == null ? exception.getClass().getSimpleName()
                            : exception.getMessage())));
        }
    }

    private static Assembly assemble(String source, Opcodes opcodes) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        try {
            DexBuilder dexBuilder = new DexBuilder(opcodes);
            smaliFlexLexer lexer = new smaliFlexLexer(new StringReader(source), opcodes.api);
            lexer.setSuppressErrors(true);
            CommonTokenStream tokens = new CommonTokenStream((TokenSource) lexer);
            tokens.fill();
            for (Object value : tokens.getTokens()) {
                if (value instanceof InvalidToken invalid) {
                    diagnostics.add(new Diagnostic(invalid.getLine(),
                            invalid.getCharPositionInLine() + 1, "error", invalid.getMessage()));
                }
            }
            if (!diagnostics.isEmpty()) return new Assembly(null, List.copyOf(diagnostics));

            CollectingParser parser = new CollectingParser(tokens, diagnostics);
            parser.setVerboseErrors(true);
            parser.setAllowOdex(false);
            parser.setApiLevel(opcodes.api);
            smaliParser.smali_file_return parsed = parser.smali_file();
            if (!diagnostics.isEmpty() || parser.getNumberOfSyntaxErrors() > 0) {
                return new Assembly(null, List.copyOf(diagnostics));
            }

            CommonTreeNodeStream tree = new CommonTreeNodeStream((CommonTree) parsed.getTree());
            tree.setTokenStream(tokens);
            CollectingTreeWalker walker = new CollectingTreeWalker(tree, diagnostics);
            walker.setApiLevel(opcodes.api);
            walker.setVerboseErrors(true);
            walker.setDexBuilder(dexBuilder);
            walker.smali_file();
            if (!diagnostics.isEmpty() || walker.getNumberOfSyntaxErrors() > 0) {
                return new Assembly(null, List.copyOf(diagnostics));
            }

            MemoryDataStore data = new MemoryDataStore();
            dexBuilder.writeTo(data);
            DexBackedDexFile dexFile = new DexBackedDexFile(opcodes, data.getData());
            if (dexFile.getClasses().size() != 1) {
                diagnostics.add(new Diagnostic(0, 0, "error",
                        "Smali must declare exactly one class"));
                return new Assembly(null, List.copyOf(diagnostics));
            }
            return new Assembly(dexFile.getClasses().iterator().next(), List.of());
        } catch (RecognitionException exception) {
            diagnostics.add(diagnostic(exception, exception.getMessage()));
        } catch (Exception exception) {
            diagnostics.add(new Diagnostic(0, 0, "error",
                    exception.getMessage() == null ? exception.getClass().getSimpleName()
                            : exception.getMessage()));
        }
        return new Assembly(null, List.copyOf(diagnostics));
    }

    private static Method findMethod(ClassDef owner, BrowseService.MemberId method) {
        for (Method candidate : owner.getMethods()) {
            if (candidate.getName().equals(method.name())
                    && DexDescriptors.methodDescriptor(candidate).equals(method.descriptor())) {
                return candidate;
            }
        }
        return null;
    }

    private static Diagnostic diagnostic(RecognitionException exception, String message) {
        return new Diagnostic(exception.line, exception.charPositionInLine + 1, "error",
                message == null ? "Invalid smali" : message);
    }

    private static String fingerprint(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static final class CollectingParser extends smaliParser {
        private final List<Diagnostic> diagnostics;

        private CollectingParser(TokenStream input, List<Diagnostic> diagnostics) {
            super(input);
            this.diagnostics = diagnostics;
        }

        @Override
        public void displayRecognitionError(String[] tokenNames, RecognitionException exception) {
            diagnostics.add(diagnostic(exception, getErrorMessage(exception, tokenNames)));
        }
    }

    private static final class CollectingTreeWalker extends smaliTreeWalker {
        private final List<Diagnostic> diagnostics;

        private CollectingTreeWalker(TreeNodeStream input, List<Diagnostic> diagnostics) {
            super(input);
            this.diagnostics = diagnostics;
        }

        @Override
        public void displayRecognitionError(String[] tokenNames, RecognitionException exception) {
            diagnostics.add(diagnostic(exception, getErrorMessage(exception, tokenNames)));
        }
    }

    public record Diagnostic(int line, int column, String severity, String message) {
    }

    public record Candidate(boolean valid, String dexFile, String target, byte[] bytes,
                            String fingerprint, List<Diagnostic> diagnostics) {
        public Candidate {
            diagnostics = !valid && diagnostics.isEmpty()
                    ? List.of(new Diagnostic(0, 0, "error", "Invalid smali"))
                    : List.copyOf(diagnostics);
        }

        private static Candidate invalid(DexClassEntry target, List<Diagnostic> diagnostics) {
            return new Candidate(false, target.getFile().getName(), target.getInternalName(),
                    null, null, diagnostics);
        }
    }

    private record Assembly(ClassDef classDef, List<Diagnostic> diagnostics) {
    }
}
