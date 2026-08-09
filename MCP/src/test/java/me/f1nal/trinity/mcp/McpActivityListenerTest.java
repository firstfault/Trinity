package me.f1nal.trinity.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import me.f1nal.trinity.application.TrinityApplication;
import me.f1nal.trinity.application.TrinityStatus;
import me.f1nal.trinity.mcp.tools.StatusTool;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpActivityListenerTest {
    @Test
    void recordsLifecycleAndToolEvents() throws Exception {
        TrinityStatus status = new TrinityStatus("test-version",
                new TrinityStatus.ProjectStatus("sample", "C:/sample.tdb", true, null, 100, 1, 0, 0));
        TrinityApplication application = new TestTrinityApplication(status);
        List<McpActivityEvent> events = new CopyOnWriteArrayList<>();
        McpActivityListener listener = events::add;

        try (TrinityMcpServer server = new TrinityMcpServer(application, "127.0.0.1", 0, listener)) {
            server.start();
            var transport = HttpClientStreamableHttpTransport
                    .builder("http://127.0.0.1:" + server.port())
                    .endpoint(TrinityMcpServer.ENDPOINT).build();
            var client = McpClient.sync(transport)
                    .clientInfo(new McpSchema.Implementation("test-agent", "9.9"))
                    .build();
            try {
                client.initialize();
                client.callTool(McpSchema.CallToolRequest.builder(StatusTool.NAME).build());
                client.callTool(McpSchema.CallToolRequest.builder("class_get")
                        .arguments(Map.of("internalName", "")).build());
            } finally {
                client.closeGracefully();
            }

            List<McpActivityEvent.Type> types = events.stream().map(McpActivityEvent::getType).toList();
            assertTrue(types.contains(McpActivityEvent.Type.SERVER_STARTED),
                    "expected SERVER_STARTED, got " + types);
            assertTrue(types.contains(McpActivityEvent.Type.AGENT_CONNECTED),
                    "expected AGENT_CONNECTED, got " + types);
            assertTrue(types.contains(McpActivityEvent.Type.TOOL_CALLED),
                    "expected TOOL_CALLED, got " + types);

            McpActivityEvent started = first(events, McpActivityEvent.Type.SERVER_STARTED);
            assertTrue(started.getMessage().contains("http://"), started.getMessage());

            List<McpActivityEvent> toolCalls = all(events, McpActivityEvent.Type.TOOL_CALLED);
            assertEquals(2, toolCalls.size(), "expected two tool calls");
            assertEquals("test-agent", toolCalls.get(0).getAgentName());
            assertEquals("9.9", toolCalls.get(0).getAgentVersion());
            assertEquals(StatusTool.NAME, toolCalls.get(0).getToolName());

            List<McpActivityEvent> results = all(events, McpActivityEvent.Type.TOOL_RESULT);
            assertEquals(2, results.size());
            assertTrue(results.get(0).isSuccess());
            assertFalse(results.get(1).isSuccess());
            assertTrue(results.get(1).getDetail().contains("INVALID_INPUT"),
                    results.get(1).getDetail());

            assertEquals(1, server.getConnectedAgents().size());
            assertEquals("test-agent", server.getConnectedAgents().values().iterator().next().getName());
        }

        List<McpActivityEvent.Type> afterClose = events.stream().map(McpActivityEvent::getType).toList();
        assertTrue(afterClose.contains(McpActivityEvent.Type.SERVER_STOPPED),
                "expected SERVER_STOPPED, got " + afterClose);
    }

    private static McpActivityEvent first(List<McpActivityEvent> events, McpActivityEvent.Type type) {
        return all(events, type).get(0);
    }

    private static List<McpActivityEvent> all(List<McpActivityEvent> events, McpActivityEvent.Type type) {
        List<McpActivityEvent> matches = new ArrayList<>();
        for (McpActivityEvent event : events) {
            if (event.getType() == type) matches.add(event);
        }
        return matches;
    }
}
