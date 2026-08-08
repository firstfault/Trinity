package me.f1nal.trinity.mcp;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
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
import java.util.Objects;

/** Embedded loopback-only Streamable HTTP MCP server. */
public final class TrinityMcpServer implements AutoCloseable {
    public static final String ENDPOINT = "/mcp";

    private final TrinityApplication application;
    private final String host;
    private final int configuredPort;
    private final McpJsonMapper jsonMapper;
    private final List<IMcpToolAdapter> tools;

    private Server httpServer;
    private ServerConnector connector;
    private McpSyncServer mcpServer;

    public TrinityMcpServer(TrinityApplication application, String host, int port) {
        this.application = Objects.requireNonNull(application, "application");
        this.host = requireLoopback(host);
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        this.configuredPort = port;
        this.jsonMapper = McpJsonDefaults.getMapper();
        this.tools = new McpToolRegistry()
                .register(new StatusTool(application, jsonMapper))
                .registerAll(ProjectTools.create(application, jsonMapper))
                .registerAll(BrowseTools.create(application, jsonMapper))
                .registerAll(AnalysisTools.create(application, jsonMapper))
                .registerAll(DexTools.create(application, jsonMapper))
                .registerAll(MutationTools.create(application, jsonMapper))
                .tools();
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
            serverBuilder.toolCall(tool.definition(), (exchange, request) -> tool.call(request));
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
            close();
            throw exception;
        }
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
}
