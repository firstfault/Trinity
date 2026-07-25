package me.f1nal.trinity.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import me.f1nal.trinity.application.ApplicationException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Reusable protocol shell; business behavior remains in application service interfaces. */
final class JsonToolAdapter implements IMcpToolAdapter {
    private final McpJsonMapper mapper;
    private final McpSchema.Tool definition;
    private final Function<Map<String, Object>, Object> handler;

    JsonToolAdapter(String name, String title, String description, Map<String, Object> inputSchema,
                    boolean readOnly, boolean destructive, boolean idempotent, boolean openWorld,
                    McpJsonMapper mapper, Function<Map<String, Object>, Object> handler) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.handler = Objects.requireNonNull(handler, "handler");
        this.definition = McpSchema.Tool.builder(name, inputSchema)
                .title(title)
                .description(description)
                .annotations(McpSchema.ToolAnnotations.builder()
                        .readOnlyHint(readOnly)
                        .destructiveHint(destructive)
                        .idempotentHint(idempotent)
                        .openWorldHint(openWorld)
                        .build())
                .build();
    }

    @Override
    public McpSchema.Tool definition() {
        return definition;
    }

    @Override
    public McpSchema.CallToolResult call(McpSchema.CallToolRequest request) {
        try {
            Object result = handler.apply(request.arguments() == null ? Map.of() : request.arguments());
            return success(result);
        } catch (ApplicationException exception) {
            return failure(exception.code().name(), exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return failure(ApplicationException.Code.INVALID_INPUT.name(), exception.getMessage());
        } catch (RuntimeException exception) {
            return failure(ApplicationException.Code.INTERNAL_ERROR.name(),
                    exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }

    private McpSchema.CallToolResult success(Object value) {
        try {
            String json = mapper.writeValueAsString(value);
            return McpSchema.CallToolResult.builder()
                    .textContent(List.of(json))
                    .structuredContent(mapper, json)
                    .build();
        } catch (IOException exception) {
            return failure(ApplicationException.Code.INTERNAL_ERROR.name(),
                    "Failed to serialize tool result: " + exception.getMessage());
        }
    }

    private McpSchema.CallToolResult failure(String code, String message) {
        Map<String, Object> body = Map.of("error", Map.of(
                "code", code,
                "message", message == null ? code : message));
        try {
            String json = mapper.writeValueAsString(body);
            return McpSchema.CallToolResult.builder()
                    .textContent(List.of(json))
                    .structuredContent(mapper, json)
                    .isError(true)
                    .build();
        } catch (IOException exception) {
            return McpSchema.CallToolResult.builder()
                    .textContent(List.of(code + ": " + message))
                    .isError(true)
                    .build();
        }
    }
}
