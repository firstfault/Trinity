package me.f1nal.trinity.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import me.f1nal.trinity.application.MutationService;
import me.f1nal.trinity.application.TrinityApplication;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static me.f1nal.trinity.mcp.tools.ToolSupport.*;

/** MCP presentation adapters for every revision-protected mutation use case. */
public final class MutationTools {
    private MutationTools() {
    }

    public static List<IMcpToolAdapter> create(TrinityApplication application, McpJsonMapper mapper) {
        MutationService mutations = application.mutations();
        return List.of(
                new JsonToolAdapter("name_set", "Set Display Name",
                        "Assigns an analytical name to a class, method, field, package, or resource.",
                        object(nameProperties(true), "kind", "newName", "expectedRevision"),
                        false, false, false, false, mapper, args -> mutations.setName(
                        new MutationService.NameMutation(nameTarget(args),
                                requiredString(args, "newName"), requiredLong(args, "expectedRevision")))),
                new JsonToolAdapter("name_revert", "Revert Display Name",
                        "Reverts a class, method, or field display name to its retained original name.",
                        object(nameProperties(false), "kind", "expectedRevision"),
                        false, false, true, false, mapper, args -> mutations.revertName(
                        nameTarget(args), requiredLong(args, "expectedRevision"))),
                new JsonToolAdapter("resource_create", "Create Resource",
                        "Creates a project resource from base64, UTF-8 text, or hexadecimal content.",
                        object(properties(
                                "path", string("New relative archive path"),
                                "encoding", string("base64, utf8/text, or hex; defaults to base64"),
                                "content", string("Encoded resource content"),
                                "expectedRevision", integer("Required current project revision")),
                                "path", "content", "expectedRevision"),
                        false, false, false, false, mapper, args -> mutations.createResource(
                        new MutationService.ResourceMutation(requiredString(args, "path"),
                                optionalString(args, "encoding", "base64"),
                                optionalString(args, "content", ""),
                                requiredLong(args, "expectedRevision")))),
                new JsonToolAdapter("resource_delete", "Delete Resource",
                        "Deletes one exact project resource after checking the project revision.",
                        object(properties(
                                "path", string("Exact resource archive path"),
                                "expectedRevision", integer("Required current project revision")),
                                "path", "expectedRevision"),
                        false, true, true, false, mapper, args -> mutations.deleteResource(
                        requiredString(args, "path"), requiredLong(args, "expectedRevision"))),
                new JsonToolAdapter("method_validate_bytecode", "Validate Method Bytecode",
                        "Parses and validates complete canonical assembler text without mutating the method.",
                        bytecodeSchema(), true, false, true, false, mapper,
                        args -> mutations.validateBytecode(bytecode(args))),
                new JsonToolAdapter("method_replace_bytecode", "Replace Method Bytecode",
                        "Atomically validates and replaces a method's canonical assembler instruction sequence.",
                        bytecodeSchema(), false, true, false, false, mapper,
                        args -> mutations.replaceBytecode(bytecode(args))),
                new JsonToolAdapter("refactor_preview", "Preview Automated Refactor",
                        "Generates a revision-bound full, enum-field, or Sponge Mixin rename plan.",
                        object(properties(
                                "mode", string("full, enum_fields, or mixins"),
                                "mixinPackage", string("Destination package used by mixins mode"),
                                "expectedRevision", integer("Required current project revision")),
                                "mode", "expectedRevision"),
                        true, false, false, false, mapper, args -> mutations.previewRefactor(
                        new MutationService.RefactorRequest(requiredString(args, "mode"),
                                optionalString(args, "mixinPackage", null),
                                requiredLong(args, "expectedRevision")))),
                new JsonToolAdapter("refactor_apply", "Apply Automated Refactor",
                        "Applies a previously generated refactor preview exactly once if its revision is still current.",
                        object(properties(
                                "previewToken", string("Token returned by refactor_preview"),
                                "expectedRevision", integer("Revision returned by refactor_preview")),
                                "previewToken", "expectedRevision"),
                        false, true, false, false, mapper, args -> mutations.applyRefactor(
                        new MutationService.ApplyRefactor(requiredString(args, "previewToken"),
                                requiredLong(args, "expectedRevision")))));
    }

    private static Map<String, Object> nameProperties(boolean includeNewName) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("kind", string("class, method, field, resource, or package"));
        output.put("owner", string("Class target or exact member owner"));
        output.put("name", string("Exact real member name"));
        output.put("descriptor", string("Exact JVM member descriptor"));
        output.put("path", string("Resource or package path"));
        if (includeNewName) output.put("newName", string("New display name or path"));
        output.put("expectedRevision", integer("Required current project revision"));
        return output;
    }

    private static Map<String, Object> bytecodeSchema() {
        Map<String, Object> output = new LinkedHashMap<>(memberProperties());
        output.put("instructions", string("Complete canonical trinity-assembler-v1 instruction text"));
        output.put("maxStack", integer("Optional replacement max-stack value"));
        output.put("maxLocals", integer("Optional replacement max-locals value"));
        output.put("expectedRevision", integer("Required current project revision"));
        return object(output, "owner", "name", "descriptor", "instructions", "expectedRevision");
    }
}
