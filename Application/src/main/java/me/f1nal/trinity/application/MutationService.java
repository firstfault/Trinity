package me.f1nal.trinity.application;

import java.util.List;

/** Revision-protected headless naming, resource, bytecode, and refactor mutations. */
public interface MutationService {
    MutationResult setName(NameMutation command);

    MutationResult revertName(NameTarget target, long expectedRevision);

    MutationResult createResource(ResourceMutation command);

    MutationResult deleteResource(String path, long expectedRevision);

    BytecodeValidation validateBytecode(BytecodeCommand command);

    MutationResult replaceBytecode(BytecodeCommand command);

    RefactorPreview previewRefactor(RefactorRequest request);

    MutationResult applyRefactor(ApplyRefactor command);

    record NameTarget(String kind, String owner, String name, String descriptor,
                      String path) {
    }

    record NameMutation(NameTarget target, String newName, long expectedRevision) {
    }

    record ResourceMutation(String path, String encoding, String content,
                            long expectedRevision) {
    }

    record BytecodeCommand(BrowseService.MemberId method, String instructions,
                           Integer maxStack, Integer maxLocals,
                           long expectedRevision) {
    }

    record RefactorRequest(String mode, String mixinPackage, long expectedRevision) {
    }

    record ApplyRefactor(String previewToken, long expectedRevision) {
    }

    record MutationResult(String operation, String target, long previousRevision,
                          long revision, List<String> changedTargets) {
        public MutationResult {
            changedTargets = List.copyOf(changedTargets);
        }
    }

    record BytecodeValidation(boolean valid, String fingerprint,
                              List<String> errors, List<String> warnings,
                              int instructionCount, Integer computedMaxStack,
                              Integer computedMaxLocals, long revision) {
        public BytecodeValidation {
            errors = List.copyOf(errors);
            warnings = List.copyOf(warnings);
        }
    }

    record ProposedRename(NameTarget target, String currentName, String proposedName) {
    }

    record RefactorPreview(String token, String mode, List<ProposedRename> renames,
                           long revision) {
        public RefactorPreview {
            renames = List.copyOf(renames);
        }
    }
}
