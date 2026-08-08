package me.f1nal.trinity.mcp;

import me.f1nal.trinity.mcp.tools.IMcpToolAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Ordered MCP tool catalog that rejects duplicate protocol names. */
final class McpToolRegistry {
    private final List<IMcpToolAdapter> tools = new ArrayList<>();
    private final Set<String> names = new HashSet<>();

    McpToolRegistry register(IMcpToolAdapter tool) {
        Objects.requireNonNull(tool, "tool");
        String name = Objects.requireNonNull(tool.definition(), "tool definition").name();
        if (!names.add(name)) {
            throw new IllegalArgumentException(String.format("Duplicate MCP tool: %s", name));
        }
        tools.add(tool);
        return this;
    }

    McpToolRegistry registerAll(Collection<? extends IMcpToolAdapter> tools) {
        tools.forEach(this::register);
        return this;
    }

    List<IMcpToolAdapter> tools() {
        return List.copyOf(tools);
    }
}
