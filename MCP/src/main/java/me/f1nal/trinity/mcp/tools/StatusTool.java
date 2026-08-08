package me.f1nal.trinity.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import me.f1nal.trinity.application.TrinityApplication;
import me.f1nal.trinity.application.TrinityStatus;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StatusTool implements IMcpToolAdapter {
    public static final String NAME = "trinity_status";
    private static final Map<String, Object> INPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(),
            "additionalProperties", false);

    private final TrinityApplication application;
    private final McpJsonMapper jsonMapper;
    private final McpSchema.Tool definition;

    public StatusTool(TrinityApplication application, McpJsonMapper jsonMapper) {
        this.application = Objects.requireNonNull(application, "application");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
        this.definition = McpSchema.Tool.builder(NAME, INPUT_SCHEMA)
                .title("Trinity Status")
                .description("Returns the Trinity version, project state, loading progress, and workspace counts.")
                .annotations(McpSchema.ToolAnnotations.builder()
                        .readOnlyHint(true)
                        .destructiveHint(false)
                        .idempotentHint(true)
                        .openWorldHint(false)
                        .build())
                .build();
    }

    @Override
    public McpSchema.Tool definition() {
        return definition;
    }

    @Override
    public McpSchema.CallToolResult call(McpSchema.CallToolRequest request) {
        TrinityStatus status = application.status();
        try {
            String json = jsonMapper.writeValueAsString(status);
            return McpSchema.CallToolResult.builder()
                    .textContent(List.of(json))
                    .structuredContent(jsonMapper, json)
                    .build();
        } catch (IOException exception) {
            return McpSchema.CallToolResult.builder()
                    .textContent(List.of("Failed to serialize Trinity status: " + exception.getMessage()))
                    .isError(true)
                    .build();
        }
    }
}
