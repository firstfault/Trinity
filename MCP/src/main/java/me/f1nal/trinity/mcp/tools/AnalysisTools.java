package me.f1nal.trinity.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import me.f1nal.trinity.application.AnalysisService;
import me.f1nal.trinity.application.TrinityApplication;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static me.f1nal.trinity.mcp.tools.ToolSupport.*;

/** MCP presentation adapters for xrefs, constants, patterns, and invocation analysis. */
public final class AnalysisTools {
    private AnalysisTools() {
    }

    public static List<IMcpToolAdapter> create(TrinityApplication application, McpJsonMapper mapper) {
        AnalysisService analysis = application.analysis();
        return List.of(
                new JsonToolAdapter("xref_find_class", "Find Class References",
                        "Finds paginated references to an exact JVM internal class name.",
                        object(properties(
                                "internalName", string("Exact JVM internal class name"),
                                "offset", integer("Zero-based result offset"),
                                "limit", integer("Maximum results, capped at 500")), "internalName"),
                        true, false, true, false, mapper, args -> analysis.findClassReferences(
                        new AnalysisService.ClassReferenceQuery(requiredString(args, "internalName"),
                                integer(args, "offset", 0), integer(args, "limit", 100)))),
                new JsonToolAdapter("xref_find_member", "Find Member References",
                        "Finds bytecode references to an exact field or method identity.",
                        object(memberAndPagination(), "owner", "name", "descriptor"),
                        true, false, true, false, mapper, args -> analysis.findMemberReferences(
                        new AnalysisService.MemberReferenceQuery(member(args),
                                integer(args, "offset", 0), integer(args, "limit", 100)))),
                new JsonToolAdapter("constant_search", "Search Constants",
                        "Searches LDC, primitive constant, immediate integer, and increment operands.",
                        object(properties(
                                "type", string("all, string, number, integer, long, float, double, type, or handle"),
                                "value", string("Optional textual value filter"),
                                "exact", bool("Require an exact textual match"),
                                "caseSensitive", bool("Use case-sensitive text matching"),
                                "offset", integer("Zero-based result offset"),
                                "limit", integer("Maximum results, capped at 500"))),
                        true, false, true, false, mapper, args -> analysis.searchConstants(
                        new AnalysisService.ConstantQuery(optionalString(args, "type", "all"),
                                optionalString(args, "value", ""), bool(args, "exact", true),
                                bool(args, "caseSensitive", false), integer(args, "offset", 0),
                                integer(args, "limit", 100)))),
                new JsonToolAdapter("pattern_validate", "Validate Bytecode Pattern",
                        "Compiles Trinity's typed assembler-pattern language and returns precise diagnostics.",
                        object(properties(
                                "pattern", string("Typed instruction pattern source"),
                                "includeMetadata", bool("Include labels, frames, and line-number nodes")), "pattern"),
                        true, false, true, false, mapper, args -> analysis.validatePattern(
                        new AnalysisService.PatternQuery(requiredString(args, "pattern"),
                                bool(args, "includeMetadata", false)))),
                new JsonToolAdapter("pattern_search", "Search Bytecode Pattern",
                        "Searches every method, or one owner class, with a compiled instruction pattern.",
                        object(properties(
                                "pattern", string("Typed instruction pattern source"),
                                "includeMetadata", bool("Include labels, frames, and line-number nodes"),
                                "owner", string("Optional exact owner class restriction"),
                                "offset", integer("Zero-based result offset"),
                                "limit", integer("Maximum results, capped at 500")), "pattern"),
                        true, false, true, false, mapper, args -> analysis.searchPattern(
                        new AnalysisService.PatternSearch(requiredString(args, "pattern"),
                                bool(args, "includeMetadata", false),
                                optionalString(args, "owner", null), integer(args, "offset", 0),
                                integer(args, "limit", 100)))),
                new JsonToolAdapter("invocation_get_details", "Get Invocation Details",
                        "Decodes one method or invokedynamic instruction at an exact instruction index.",
                        object(invocationProperties(), "owner", "name", "descriptor", "instructionIndex"),
                        true, false, true, false, mapper, args -> analysis.getInvocation(
                        new AnalysisService.InvocationQuery(member(args),
                                integer(args, "instructionIndex", -1)))));
    }

    private static Map<String, Object> memberAndPagination() {
        Map<String, Object> output = new LinkedHashMap<>(memberProperties());
        output.putAll(paginationProperties());
        return output;
    }

    private static Map<String, Object> invocationProperties() {
        Map<String, Object> output = new LinkedHashMap<>(memberProperties());
        output.put("instructionIndex", integer("Zero-based index in the full ASM instruction list"));
        return output;
    }
}
