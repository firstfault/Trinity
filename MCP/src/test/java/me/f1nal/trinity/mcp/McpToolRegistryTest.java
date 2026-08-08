package me.f1nal.trinity.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import me.f1nal.trinity.mcp.tools.IMcpToolAdapter;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpToolRegistryTest {
    @Test
    void preservesRegistrationOrder() {
        IMcpToolAdapter first = tool("first");
        IMcpToolAdapter second = tool("second");

        McpToolRegistry registry = new McpToolRegistry()
                .register(first)
                .register(second);

        assertEquals(java.util.List.of(first, second), registry.tools());
    }

    @Test
    void rejectsDuplicateProtocolNames() {
        McpToolRegistry registry = new McpToolRegistry().register(tool("duplicate"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> registry.register(tool("duplicate")));

        assertEquals("Duplicate MCP tool: duplicate", exception.getMessage());
    }

    private static IMcpToolAdapter tool(String name) {
        McpSchema.Tool definition = McpSchema.Tool.builder(name,
                Map.of("type", "object", "properties", Map.of())).build();
        return new IMcpToolAdapter() {
            @Override
            public McpSchema.Tool definition() {
                return definition;
            }

            @Override
            public McpSchema.CallToolResult call(McpSchema.CallToolRequest request) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
