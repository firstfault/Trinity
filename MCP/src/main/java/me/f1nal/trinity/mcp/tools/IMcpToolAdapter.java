package me.f1nal.trinity.mcp.tools;

import io.modelcontextprotocol.spec.McpSchema;

/** Protocol adapter for one application capability. */
public interface IMcpToolAdapter {
    McpSchema.Tool definition();

    McpSchema.CallToolResult call(McpSchema.CallToolRequest request);
}
