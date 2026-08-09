package me.f1nal.trinity.mcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Render-thread-facing registry of MCP server activity. The MCP server runs on
 * Jetty threads and publishes events here; the GUI reads snapshots from the
 * render thread.
 */
public final class McpActivityLog implements McpActivityListener {
    public static final int MAX_EVENTS = 1024;

    public record AgentSnapshot(String sessionId, String name, String version, long lastSeenMillis) { }

    private final List<McpActivityEvent> events = new ArrayList<>();
    private final Map<String, AgentSnapshot> agents = new LinkedHashMap<>();
    private boolean serverRunning;
    private String endpoint = "";
    private int totalToolCalls;
    private int failedToolCalls;
    private long lastEventMillis;
    private int revision;

    @Override
    public synchronized void publish(McpActivityEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.getType() == McpActivityEvent.Type.SERVER_STARTED) {
            serverRunning = true;
            endpoint = extractEndpoint(event.getMessage());
        } else if (event.getType() == McpActivityEvent.Type.SERVER_STOPPED
                || event.getType() == McpActivityEvent.Type.SERVER_FAILED) {
            serverRunning = false;
            if (event.getType() == McpActivityEvent.Type.SERVER_STOPPED) {
                agents.clear();
            }
        } else if (event.getType() == McpActivityEvent.Type.AGENT_CONNECTED) {
            agents.put(event.getSessionId(), new AgentSnapshot(
                    event.getSessionId(), event.getAgentName(), event.getAgentVersion(),
                    event.getTimestamp()));
        } else if (event.getType() == McpActivityEvent.Type.AGENT_DISCONNECTED) {
            agents.remove(event.getSessionId());
        } else if (event.getType() == McpActivityEvent.Type.TOOL_CALLED) {
            if (!event.getSessionId().isEmpty()) {
                agents.put(event.getSessionId(), new AgentSnapshot(
                        event.getSessionId(), event.getAgentName(), event.getAgentVersion(),
                        event.getTimestamp()));
            }
            totalToolCalls++;
        } else if (event.getType() == McpActivityEvent.Type.TOOL_RESULT) {
            if (!event.isSuccess()) failedToolCalls++;
        }

        events.add(event);
        while (events.size() > MAX_EVENTS) {
            events.remove(0);
        }
        lastEventMillis = event.getTimestamp();
        revision++;
    }

    public synchronized boolean isServerRunning() {
        return serverRunning;
    }

    public synchronized String getEndpoint() {
        return endpoint;
    }

    public synchronized int getTotalToolCalls() {
        return totalToolCalls;
    }

    public synchronized int getFailedToolCalls() {
        return failedToolCalls;
    }

    public synchronized long getLastEventMillis() {
        return lastEventMillis;
    }

    public synchronized int getRevision() {
        return revision;
    }

    public synchronized List<McpActivityEvent> snapshotEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public synchronized List<AgentSnapshot> snapshotAgents() {
        return Collections.unmodifiableList(new ArrayList<>(agents.values()));
    }

    public synchronized void clear() {
        events.clear();
        totalToolCalls = 0;
        failedToolCalls = 0;
        lastEventMillis = 0L;
        revision++;
    }

    private static String extractEndpoint(String message) {
        int at = message.lastIndexOf("http");
        return at < 0 ? "" : message.substring(at).trim();
    }
}
