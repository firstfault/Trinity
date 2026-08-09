package me.f1nal.trinity.mcp;

/** Sink for {@link McpActivityEvent}s produced by the embedded MCP server. */
@FunctionalInterface
public interface McpActivityListener {
    McpActivityListener NOP = event -> { };

    void publish(McpActivityEvent event);
}
