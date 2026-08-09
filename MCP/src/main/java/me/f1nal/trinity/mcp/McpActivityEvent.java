package me.f1nal.trinity.mcp;

import java.util.Objects;

/**
 * Immutable record of one notable MCP server event such as a client connecting,
 * a tool being invoked, or the server lifecycle changing.
 */
public final class McpActivityEvent {
    public enum Type {
        SERVER_STARTED,
        SERVER_STOPPED,
        SERVER_FAILED,
        AGENT_CONNECTED,
        AGENT_DISCONNECTED,
        TOOL_CALLED,
        TOOL_RESULT
    }

    private final long timestamp;
    private final Type type;
    private final String sessionId;
    private final String agentName;
    private final String agentVersion;
    private final String toolName;
    private final String message;
    private final String detail;
    private final boolean success;
    private final long durationMillis;

    public McpActivityEvent(long timestamp, Type type, String sessionId, String agentName,
                            String agentVersion, String toolName, String message, String detail,
                            boolean success, long durationMillis) {
        this.timestamp = timestamp;
        this.type = Objects.requireNonNull(type, "type");
        this.sessionId = Objects.requireNonNullElse(sessionId, "");
        this.agentName = Objects.requireNonNullElse(agentName, "");
        this.agentVersion = Objects.requireNonNullElse(agentVersion, "");
        this.toolName = Objects.requireNonNullElse(toolName, "");
        this.message = Objects.requireNonNullElse(message, "");
        this.detail = Objects.requireNonNullElse(detail, "");
        this.success = success;
        this.durationMillis = durationMillis;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Type getType() {
        return type;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public String getToolName() {
        return toolName;
    }

    public String getMessage() {
        return message;
    }

    public String getDetail() {
        return detail;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getDurationMillis() {
        return durationMillis;
    }
}
