package me.f1nal.trinity.application;

import java.util.List;

/** Headless cross-reference, constant, instruction-pattern, and invocation analysis. */
public interface AnalysisService {
    Page<XrefResult> findClassReferences(ClassReferenceQuery query);

    Page<XrefResult> findMemberReferences(MemberReferenceQuery query);

    Page<ConstantResult> searchConstants(ConstantQuery query);

    PatternValidation validatePattern(PatternQuery query);

    Page<PatternMatch> searchPattern(PatternSearch query);

    InvocationDetails getInvocation(InvocationQuery query);

    record ClassReferenceQuery(String internalName, int offset, int limit) {
    }

    record MemberReferenceQuery(BrowseService.MemberId member, int offset, int limit) {
    }

    record ConstantQuery(String type, String value, boolean exact,
                         boolean caseSensitive, int offset, int limit) {
    }

    record PatternQuery(String pattern, boolean includeMetadata) {
    }

    record PatternSearch(String pattern, boolean includeMetadata,
                         String owner, int offset, int limit) {
    }

    record InvocationQuery(BrowseService.MemberId caller, int instructionIndex) {
    }

    record XrefResult(String kind, String access, String invocation,
                      BrowseService.MemberId caller, Integer instructionIndex,
                      String locationText) {
    }

    record ConstantResult(String type, Object value, BrowseService.MemberId method,
                          int instructionIndex, String instruction) {
    }

    record PatternValidation(boolean valid, int instructionPatternCount,
                             List<PatternDiagnostic> diagnostics) {
        public PatternValidation {
            diagnostics = List.copyOf(diagnostics);
        }
    }

    record PatternDiagnostic(int line, int column, String severity, String message) {
    }

    record PatternMatch(BrowseService.MemberId method, int startInstructionIndex,
                        int endInstructionIndex, String matchedInstructions) {
    }

    record InvocationDetails(BrowseService.MemberId caller, int instructionIndex,
                             String opcode, String functionKind, String owner,
                             String name, String descriptor, boolean interfaceOwner,
                             boolean resolvedInProject, BrowseService.MemberId resolvedTarget,
                             String returnType, List<String> parameterTypes,
                             List<Object> bootstrapArguments, long revision) {
        public InvocationDetails {
            parameterTypes = List.copyOf(parameterTypes);
            bootstrapArguments = List.copyOf(bootstrapArguments);
        }
    }
}
