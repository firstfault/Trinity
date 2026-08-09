package me.f1nal.trinity.mcp;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import me.f1nal.trinity.application.TrinityApplication;
import me.f1nal.trinity.mcp.tools.AnalysisTools;
import me.f1nal.trinity.mcp.tools.BrowseTools;
import me.f1nal.trinity.mcp.tools.DexTools;
import me.f1nal.trinity.mcp.tools.IMcpToolAdapter;
import me.f1nal.trinity.mcp.tools.MutationTools;
import me.f1nal.trinity.mcp.tools.ProjectTools;
import me.f1nal.trinity.mcp.tools.StatusTool;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Embedded loopback-only Streamable HTTP MCP server. */
public final class TrinityMcpServer implements AutoCloseable {
    public static final String ENDPOINT = "/mcp";

    private final TrinityApplication application;
    private final String host;
    private final int configuredPort;
    private final McpJsonMapper jsonMapper;
    private final List<IMcpToolAdapter> tools;
    private final McpActivityListener activityListener;
    private final Map<String, AgentInfo> connectedAgents = new ConcurrentHashMap<>();

    private Server httpServer;
    private ServerConnector connector;
    private McpSyncServer mcpServer;

    public TrinityMcpServer(TrinityApplication application, String host, int port) {
        this(application, host, port, McpActivityListener.NOP);
    }

    public TrinityMcpServer(TrinityApplication application, String host, int port,
                            McpActivityListener activityListener) {
        this.application = Objects.requireNonNull(application, "application");
        this.host = requireLoopback(host);
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        this.configuredPort = port;
        this.jsonMapper = McpJsonDefaults.getMapper();
        this.activityListener = Objects.requireNonNullElse(activityListener, McpActivityListener.NOP);
        this.tools = new McpToolRegistry()
                .register(new StatusTool(application, jsonMapper))
                .registerAll(ProjectTools.create(application, jsonMapper))
                .registerAll(BrowseTools.create(application, jsonMapper))
                .registerAll(AnalysisTools.create(application, jsonMapper))
                .registerAll(DexTools.create(application, jsonMapper))
                .registerAll(MutationTools.create(application, jsonMapper))
                .tools();
    }

    public Map<String, AgentInfo> getConnectedAgents() {
        return connectedAgents;
    }

    public synchronized void start() throws Exception {
        if (isRunning()) {
            throw new IllegalStateException("MCP server is already running");
        }

        HttpServletStreamableServerTransportProvider transport =
                HttpServletStreamableServerTransportProvider.builder()
                        .jsonMapper(jsonMapper)
                        .mcpEndpoint(ENDPOINT)
                        .build();

        var serverBuilder = McpServer.sync(transport)
                .serverInfo("trinity", application.version());
        for (IMcpToolAdapter tool : tools) {
            serverBuilder.toolCall(tool.definition(), (exchange, request) ->
                    dispatchToolCall(tool, exchange, request));
        }
        mcpServer = serverBuilder.build();

        httpServer = new Server();
        connector = new ServerConnector(httpServer);
        connector.setHost(host);
        connector.setPort(configuredPort);
        httpServer.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        ServletHolder servlet = new ServletHolder(transport);
        servlet.setAsyncSupported(true);
        context.addServlet(servlet, ENDPOINT);
        httpServer.setHandler(context);

        try {
            httpServer.start();
        } catch (Exception exception) {
            emit(new McpActivityEvent(System.currentTimeMillis(), McpActivityEvent.Type.SERVER_FAILED,
                    "", "", "", "",
                    "Failed to start MCP server",
                    Objects.requireNonNullElse(exception.getMessage(), exception.getClass().getSimpleName()),
                    false, 0L));
            close();
            throw exception;
        }

        URI endpoint = endpoint();
        connectedAgents.clear();
        emit(new McpActivityEvent(System.currentTimeMillis(), McpActivityEvent.Type.SERVER_STARTED,
                "", "", "", "",
                "MCP server listening at " + endpoint, "", true, 0L));
    }

    public synchronized boolean isRunning() {
        return httpServer != null && httpServer.isStarted();
    }

    public synchronized int port() {
        if (!isRunning()) {
            throw new IllegalStateException("MCP server is not running");
        }
        return connector.getLocalPort();
    }

    public synchronized URI endpoint() {
        if (!isRunning()) {
            throw new IllegalStateException("MCP server is not running");
        }
        try {
            return new URI("http", null, host, port(), ENDPOINT, null, null);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to construct MCP endpoint", exception);
        }
    }

    @Override
    public synchronized void close() {
        boolean wasRunning = isRunning();
        RuntimeException failure = null;
        if (mcpServer != null) {
            try {
                mcpServer.close();
            } catch (RuntimeException exception) {
                failure = exception;
            } finally {
                mcpServer = null;
            }
        }
        if (httpServer != null) {
            try {
                httpServer.stop();
            } catch (Exception exception) {
                if (failure == null) failure = new RuntimeException("Failed to stop MCP HTTP server", exception);
                else failure.addSuppressed(exception);
            } finally {
                httpServer = null;
                connector = null;
            }
        }
        if (wasRunning) {
            for (AgentInfo agent : connectedAgents.values()) {
                emit(new McpActivityEvent(System.currentTimeMillis(),
                        McpActivityEvent.Type.AGENT_DISCONNECTED,
                        agent.sessionId, agent.name, agent.version, "",
                        "Agent disconnected (server stopping)", "", false, 0L));
            }
            emit(new McpActivityEvent(System.currentTimeMillis(), McpActivityEvent.Type.SERVER_STOPPED,
                    "", "", "", "",
                    "MCP server stopped", "", true, 0L));
        }
        connectedAgents.clear();
        if (failure != null) throw failure;
    }

    private static String requireLoopback(String host) {
        Objects.requireNonNull(host, "host");
        try {
            if (!InetAddress.getByName(host).isLoopbackAddress()) {
                throw new IllegalArgumentException("MCP server must bind to a loopback address");
            }
            return host;
        } catch (java.net.UnknownHostException exception) {
            throw new IllegalArgumentException("Unknown MCP host: " + host, exception);
        }
    }

    private McpSchema.CallToolResult dispatchToolCall(IMcpToolAdapter tool,
                                                      McpSyncServerExchange exchange,
                                                      McpSchema.CallToolRequest request) {
        long started = System.currentTimeMillis();
        String sessionId = safeSessionId(exchange);
        McpSchema.Implementation clientInfo = exchange == null ? null : exchange.getClientInfo();
        String agentName = clientInfo == null ? "" : Objects.requireNonNullElse(clientInfo.name(), "");
        String agentVersion = clientInfo == null ? "" : Objects.requireNonNullElse(clientInfo.version(), "");
        trackAgent(sessionId, agentName, agentVersion);

        String toolName = tool.definition().name();
        emit(new McpActivityEvent(System.currentTimeMillis(), McpActivityEvent.Type.TOOL_CALLED,
                sessionId, agentName, agentVersion, toolName,
                "Invoked tool " + toolName, summarizeArguments(request), true, 0L));

        McpSchema.CallToolResult result;
        try {
            result = tool.call(request);
        } catch (RuntimeException exception) {
            long elapsed = System.currentTimeMillis() - started;
            emit(new McpActivityEvent(System.currentTimeMillis(), McpActivityEvent.Type.TOOL_RESULT,
                    sessionId, agentName, agentVersion, toolName,
                    "Tool " + toolName + " threw after " + elapsed + "ms",
                    Objects.requireNonNullElse(exception.getMessage(), exception.getClass().getSimpleName()),
                    false, elapsed));
            throw exception;
        }

        long elapsed = System.currentTimeMillis() - started;
        boolean isError = result != null && Boolean.TRUE.equals(result.isError());
        String outcome = isError ? "failed" : "ok";
        emit(new McpActivityEvent(System.currentTimeMillis(), McpActivityEvent.Type.TOOL_RESULT,
                sessionId, agentName, agentVersion, toolName,
                "Tool " + toolName + " " + outcome + " (" + elapsed + "ms)",
                isError ? summarizeError(result) : "",
                !isError, elapsed));
        return result;
    }

    private void trackAgent(String sessionId, String agentName, String agentVersion) {
        if (sessionId.isEmpty()) return;
        AgentInfo fresh = new AgentInfo(sessionId, agentName, agentVersion, System.currentTimeMillis());
        AgentInfo existing = connectedAgents.putIfAbsent(sessionId, fresh);
        if (existing == null) {
            emit(new McpActivityEvent(System.currentTimeMillis(), McpActivityEvent.Type.AGENT_CONNECTED,
                    sessionId, agentName, agentVersion, "",
                    "Agent connected", agentVersion.isEmpty() ? "" : "v" + agentVersion, true, 0L));
        } else if (!existing.name.equals(agentName) || !existing.version.equals(agentVersion)) {
            connectedAgents.put(sessionId, fresh);
        } else {
            existing.lastSeenMillis = System.currentTimeMillis();
        }
    }

    private static String safeSessionId(McpSyncServerExchange exchange) {
        if (exchange == null) return "";
        try {
            return Objects.requireNonNullElse(exchange.sessionId(), "");
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static String summarizeArguments(McpSchema.CallToolRequest request) {
        if (request == null) return "";
        Map<String, Object> arguments = request.arguments();
        if (arguments == null || arguments.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(entry.getKey()).append('=');
            Object value = entry.getValue();
            String text = value == null ? "null" : value.toString();
            if (text.length() > 80) text = text.substring(0, 77) + "...";
            sb.append(text);
        }
        return sb.toString();
    }

    private static String summarizeError(McpSchema.CallToolResult result) {
        if (result == null || result.content() == null || result.content().isEmpty()) return "";
        Object first = result.content().get(0);
        if (first instanceof McpSchema.TextContent text) {
            return Objects.requireNonNullElse(text.text(), "");
        }
        return first == null ? "" : first.toString();
    }

    private void emit(McpActivityEvent event) {
        try {
            activityListener.publish(event);
        } catch (RuntimeException ignored) {
            // Listeners must never break the MCP request path.
        }
    }

    /** Snapshot of a client known to be interacting with the server. */
    public static final class AgentInfo {
        private final String sessionId;
        private final String name;
        private final String version;
        private volatile long lastSeenMillis;

        AgentInfo(String sessionId, String name, String version, long lastSeenMillis) {
            this.sessionId = Objects.requireNonNullElse(sessionId, "");
            this.name = Objects.requireNonNullElse(name, "");
            this.version = Objects.requireNonNullElse(version, "");
            this.lastSeenMillis = lastSeenMillis;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public long getLastSeenMillis() {
            return lastSeenMillis;
        }
    }
}
