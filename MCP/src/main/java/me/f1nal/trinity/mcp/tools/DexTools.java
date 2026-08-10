package me.f1nal.trinity.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import me.f1nal.trinity.application.DexService;
import me.f1nal.trinity.application.TrinityApplication;

import java.util.List;

import static me.f1nal.trinity.mcp.tools.ToolSupport.bool;
import static me.f1nal.trinity.mcp.tools.ToolSupport.integer;
import static me.f1nal.trinity.mcp.tools.ToolSupport.member;
import static me.f1nal.trinity.mcp.tools.ToolSupport.object;
import static me.f1nal.trinity.mcp.tools.ToolSupport.optionalString;
import static me.f1nal.trinity.mcp.tools.ToolSupport.properties;
import static me.f1nal.trinity.mcp.tools.ToolSupport.requiredLong;
import static me.f1nal.trinity.mcp.tools.ToolSupport.requiredString;
import static me.f1nal.trinity.mcp.tools.ToolSupport.string;

/** MCP presentation adapters for native Android DEX decompilation, inspection, mutation, and analysis. */
public final class DexTools {
    private DexTools() {
    }

    public static List<IMcpToolAdapter> create(TrinityApplication application, McpJsonMapper mapper) {
        DexService dex = application.dex();
        return List.of(
                new JsonToolAdapter("dex_files", "List DEX Files",
                        "Lists original DEX payloads, API levels, byte counts, and class counts.",
                        paginationSchema(), true, false, true, false, mapper,
                        args -> dex.files(integer(args, "offset", 0), integer(args, "limit", 100))),
                new JsonToolAdapter("dex_classes", "List DEX Classes",
                        "Lists native DEX class descriptors and their containing multidex entries.",
                        object(properties(
                                "query", string("Optional internal-name text"),
                                "exact", bool("Require an exact internal-name match; defaults to false"),
                                "case_sensitive", bool("Use case-sensitive matching; defaults to false"),
                                "offset", integer("Zero-based result offset"),
                                "limit", integer("Maximum results, capped at 500"))),
                        true, false, true, false, mapper,
                        args -> dex.classes(new DexService.DexClassQuery(
                                optionalString(args, "query", ""), bool(args, "exact", false),
                                bool(args, "case_sensitive", false), integer(args, "offset", 0),
                                integer(args, "limit", 100)))),
                new JsonToolAdapter("dex_class_get", "Get DEX Class",
                        "Returns native DEX class, method, field, annotation, and source metadata.",
                        classSchema(), true, false, true, false, mapper,
                        args -> dex.getClass(requiredString(args, "internalName"))),
                new JsonToolAdapter("dex_class_disassemble", "Disassemble DEX Class",
                        "Returns lossless native baksmali output without JVM bytecode conversion.",
                        classSchema(), true, false, true, false, mapper,
                        args -> dex.disassembleClass(requiredString(args, "internalName"))),
                new JsonToolAdapter("dex_class_decompile", "Decompile DEX Class",
                        "Returns read-only Java-like source reconstructed by JADX; smali remains authoritative.",
                        classSchema(), true, false, true, false, mapper,
                        args -> dex.decompileClass(requiredString(args, "internalName"))),
                new JsonToolAdapter("dex_method_get", "Get DEX Method",
                        "Returns native Dalvik method metadata, registers, and instruction count.",
                        memberSchema(), true, false, true, false, mapper,
                        args -> dex.getMethod(member(args))),
                new JsonToolAdapter("dex_method_disassemble", "Disassemble DEX Method",
                        "Returns native baksmali for one exact DEX method identity.",
                        memberSchema(), true, false, true, false, mapper,
                        args -> dex.disassembleMethod(member(args))),
                new JsonToolAdapter("dex_method_decompile", "Decompile DEX Method",
                        "Returns one read-only Java-like method reconstructed by JADX.",
                        memberSchema(), true, false, true, false, mapper,
                        args -> dex.decompileMethod(member(args))),
                new JsonToolAdapter("dex_find_references", "Find DEX References",
                        "Finds native Dalvik type, method, or field references across all DEX files.",
                        referenceSchema(), true, false, true, false, mapper,
                        args -> dex.findReferences(new DexService.ReferenceQuery(
                                requiredString(args, "kind"), requiredString(args, "owner"),
                                optionalString(args, "name", null),
                                optionalString(args, "descriptor", null),
                                integer(args, "offset", 0), integer(args, "limit", 100)))),
                new JsonToolAdapter("dex_constant_search", "Search DEX Constants",
                        "Searches native Dalvik string and numeric constant instructions.",
                        constantSchema(), true, false, true, false, mapper,
                        args -> dex.searchConstants(new DexService.ConstantQuery(
                                optionalString(args, "type", "all"),
                                optionalString(args, "value", ""), bool(args, "exact", true),
                                bool(args, "case_sensitive", false), integer(args, "offset", 0),
                                integer(args, "limit", 100)))),
                new JsonToolAdapter("dex_class_validate_smali", "Validate DEX Class Smali",
                        "Assembles a complete native smali class and verifies a whole-DEX rebuild without mutation.",
                        classMutationSchema(), true, false, true, false, mapper,
                        args -> dex.validateClass(new DexService.DexClassMutation(
                                requiredString(args, "internalName"), requiredString(args, "smali"),
                                requiredLong(args, "expectedRevision")))),
                new JsonToolAdapter("dex_class_replace_smali", "Replace DEX Class Smali",
                        "Atomically assembles one class and rebuilds its complete containing DEX file.",
                        classMutationSchema(), false, true, false, false, mapper,
                        args -> dex.replaceClass(new DexService.DexClassMutation(
                                requiredString(args, "internalName"), requiredString(args, "smali"),
                                requiredLong(args, "expectedRevision")))),
                new JsonToolAdapter("dex_method_validate_smali", "Validate DEX Method Smali",
                        "Assembles a replacement method inside its owning class and verifies a whole-DEX rebuild.",
                        methodMutationSchema(), true, false, true, false, mapper,
                        args -> dex.validateMethod(new DexService.DexMethodMutation(
                                member(args), requiredString(args, "smali"),
                                requiredLong(args, "expectedRevision")))),
                new JsonToolAdapter("dex_method_replace_smali", "Replace DEX Method Smali",
                        "Atomically assembles one method and rebuilds its complete containing DEX file.",
                        methodMutationSchema(), false, true, false, false, mapper,
                        args -> dex.replaceMethod(new DexService.DexMethodMutation(
                                member(args), requiredString(args, "smali"),
                                requiredLong(args, "expectedRevision")))));
    }

    private static java.util.Map<String, Object> paginationSchema() {
        return object(properties(
                "offset", integer("Zero-based result offset"),
                "limit", integer("Maximum results, capped at 500")));
    }

    private static java.util.Map<String, Object> classSchema() {
        return object(properties(
                "internalName", string("Exact DEX class name such as com/example/Main")),
                "internalName");
    }

    private static java.util.Map<String, Object> memberSchema() {
        return object(properties(
                "owner", string("Exact DEX class internal name"),
                "name", string("Exact DEX member name"),
                "descriptor", string("Exact DEX method descriptor")),
                "owner", "name", "descriptor");
    }

    private static java.util.Map<String, Object> classMutationSchema() {
        return object(properties(
                "internalName", string("Exact DEX class internal name"),
                "smali", string("Complete smali class source"),
                "expectedRevision", integer("Required current project revision")),
                "internalName", "smali", "expectedRevision");
    }

    private static java.util.Map<String, Object> methodMutationSchema() {
        return object(properties(
                "owner", string("Exact DEX class internal name"),
                "name", string("Exact DEX method name"),
                "descriptor", string("Exact DEX method descriptor"),
                "smali", string("Complete smali method from .method through .end method"),
                "expectedRevision", integer("Required current project revision")),
                "owner", "name", "descriptor", "smali", "expectedRevision");
    }

    private static java.util.Map<String, Object> referenceSchema() {
        return object(properties(
                "kind", string("class, method, or field"),
                "owner", string("Exact DEX owner internal name"),
                "name", string("Required for method and field references"),
                "descriptor", string("Method descriptor or field type descriptor"),
                "offset", integer("Zero-based result offset"),
                "limit", integer("Maximum results, capped at 500")),
                "kind", "owner");
    }

    private static java.util.Map<String, Object> constantSchema() {
        return object(properties(
                "type", string("all, string, or number"),
                "value", string("Optional textual value filter"),
                "exact", bool("Require an exact textual match; defaults to true"),
                "case_sensitive", bool("Use case-sensitive string matching; defaults to false"),
                "offset", integer("Zero-based result offset"),
                "limit", integer("Maximum results, capped at 500")));
    }
}
