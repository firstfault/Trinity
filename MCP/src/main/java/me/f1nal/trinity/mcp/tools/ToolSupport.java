package me.f1nal.trinity.mcp.tools;

import me.f1nal.trinity.application.BrowseService;
import me.f1nal.trinity.application.MutationService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ToolSupport {
    private ToolSupport() {
    }

    static Map<String, Object> object(Map<String, Object> properties, String... required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.copyOf(properties));
        if (required.length > 0) schema.put("required", List.of(required));
        schema.put("additionalProperties", false);
        return Map.copyOf(schema);
    }

    static Map<String, Object> properties(Object... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("property pairs must be even");
        Map<String, Object> properties = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            properties.put((String) pairs[index], pairs[index + 1]);
        }
        return properties;
    }

    static Map<String, Object> string(String description) {
        return Map.of("type", "string", "description", description);
    }

    static Map<String, Object> integer(String description) {
        return Map.of("type", "integer", "description", description);
    }

    static Map<String, Object> bool(String description) {
        return Map.of("type", "boolean", "description", description);
    }

    static Map<String, Object> strings(String description) {
        return Map.of("type", "array", "description", description,
                "items", Map.of("type", "string"));
    }

    static String requiredString(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        if (!(value instanceof String string) || string.isBlank()) {
            throw new IllegalArgumentException(name + " must be a non-blank string");
        }
        return string;
    }

    static String optionalString(Map<String, Object> arguments, String name, String fallback) {
        Object value = arguments.get(name);
        if (value == null) return fallback;
        if (!(value instanceof String string)) throw new IllegalArgumentException(name + " must be a string");
        return string;
    }

    static int integer(Map<String, Object> arguments, String name, int fallback) {
        Object value = arguments.get(name);
        if (value == null) return fallback;
        if (!(value instanceof Number number)) throw new IllegalArgumentException(name + " must be an integer");
        return number.intValue();
    }

    static Integer optionalInteger(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        if (value == null) return null;
        if (!(value instanceof Number number)) throw new IllegalArgumentException(name + " must be an integer");
        return number.intValue();
    }

    static long requiredLong(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        if (!(value instanceof Number number)) throw new IllegalArgumentException(name + " must be an integer");
        return number.longValue();
    }

    static boolean bool(Map<String, Object> arguments, String name, boolean fallback) {
        Object value = arguments.get(name);
        if (value == null) return fallback;
        if (!(value instanceof Boolean bool)) throw new IllegalArgumentException(name + " must be a boolean");
        return bool;
    }

    static List<String> strings(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        if (!(value instanceof List<?> list)) throw new IllegalArgumentException(name + " must be an array of strings");
        List<String> output = new ArrayList<>(list.size());
        for (Object element : list) {
            if (!(element instanceof String string) || string.isBlank()) {
                throw new IllegalArgumentException(name + " must contain only non-blank strings");
            }
            output.add(string);
        }
        return List.copyOf(output);
    }

    static BrowseService.MemberId member(Map<String, Object> arguments) {
        return new BrowseService.MemberId(requiredString(arguments, "owner"),
                requiredString(arguments, "name"), requiredString(arguments, "descriptor"));
    }

    static MutationService.NameTarget nameTarget(Map<String, Object> arguments) {
        return new MutationService.NameTarget(requiredString(arguments, "kind"),
                optionalString(arguments, "owner", null), optionalString(arguments, "name", null),
                optionalString(arguments, "descriptor", null), optionalString(arguments, "path", null));
    }

    static MutationService.BytecodeCommand bytecode(Map<String, Object> arguments) {
        return new MutationService.BytecodeCommand(member(arguments),
                requiredString(arguments, "instructions"), optionalInteger(arguments, "maxStack"),
                optionalInteger(arguments, "maxLocals"), requiredLong(arguments, "expectedRevision"));
    }

    static Map<String, Object> memberProperties() {
        return properties(
                "owner", string("Exact JVM internal owner name, for example com/example/Foo"),
                "name", string("Exact real member name"),
                "descriptor", string("Exact JVM member descriptor"));
    }

    static Map<String, Object> paginationProperties() {
        return properties(
                "offset", integer("Zero-based result offset"),
                "limit", integer("Maximum results, capped at 500"));
    }
}
