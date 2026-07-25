package me.f1nal.trinity.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import me.f1nal.trinity.application.BrowseService;
import me.f1nal.trinity.application.TrinityApplication;

import java.util.List;

import static me.f1nal.trinity.mcp.tools.ToolSupport.*;

/** MCP presentation adapters for class, member, source, bytecode, hierarchy, and resource reads. */
public final class BrowseTools {
    private BrowseTools() {
    }

    public static List<IMcpToolAdapter> create(TrinityApplication application, McpJsonMapper mapper) {
        BrowseService browse = application.browse();
        return List.of(
                new JsonToolAdapter("class_get", "Get Class",
                        "Returns classfile identity, flags, signatures, relationships, annotations, and counts.",
                        classSchema(), true, false, true, false, mapper,
                        args -> browse.getClass(requiredString(args, "internalName"))),
                new JsonToolAdapter("class_get_structure", "Get Class Structure",
                        "Returns class metadata plus declared methods, fields, inner classes, and record components.",
                        classSchema(), true, false, true, false, mapper,
                        args -> browse.getClassStructure(requiredString(args, "internalName"))),
                new JsonToolAdapter("class_decompile", "Decompile Class",
                        "Returns complete Fernflower Java source for an exact internal class name.",
                        classSchema(), true, false, false, false, mapper,
                        args -> browse.decompileClass(requiredString(args, "internalName"))),
                new JsonToolAdapter("method_get", "Get Method",
                        "Returns exact method metadata, parameters, annotations, code counts, and revision.",
                        memberSchema(), true, false, true, false, mapper,
                        args -> browse.getMethod(member(args))),
                new JsonToolAdapter("method_decompile", "Decompile Method",
                        "Returns the decompiled Java source region for an exact JVM method identity.",
                        memberSchema(), true, false, false, false, mapper,
                        args -> browse.decompileMethod(member(args))),
                new JsonToolAdapter("method_get_bytecode", "Get Method Bytecode",
                        "Returns canonical lossless Trinity assembler text and a method fingerprint.",
                        memberSchema(), true, false, true, false, mapper,
                        args -> browse.getMethodBytecode(member(args))),
                new JsonToolAdapter("field_get", "Get Field",
                        "Returns exact field metadata, constant value, annotations, and revision.",
                        memberSchema(), true, false, true, false, mapper,
                        args -> browse.getField(member(args))),
                new JsonToolAdapter("resource_read", "Read Resource",
                        "Reads a project resource as base64, UTF-8 text, or hexadecimal with integrity metadata.",
                        object(properties(
                                "path", string("Exact resource archive path"),
                                "encoding", string("base64, utf8/text, or hex; defaults to base64")), "path"),
                        true, false, true, false, mapper,
                        args -> browse.readResource(requiredString(args, "path"),
                                optionalString(args, "encoding", "base64"))),
                new JsonToolAdapter("class_get_hierarchy", "Get Class Hierarchy",
                        "Returns superclasses, interfaces, subclasses, inheritors, and linked override families.",
                        classSchema(), true, false, true, false, mapper,
                        args -> browse.getClassHierarchy(requiredString(args, "internalName"))));
    }

    private static java.util.Map<String, Object> classSchema() {
        return object(properties("internalName", string("Exact JVM internal class name")), "internalName");
    }

    private static java.util.Map<String, Object> memberSchema() {
        return object(memberProperties(), "owner", "name", "descriptor");
    }
}
