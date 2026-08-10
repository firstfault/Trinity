package me.f1nal.trinity.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import me.f1nal.trinity.application.TrinityApplication;
import me.f1nal.trinity.application.TrinityStatus;
import me.f1nal.trinity.mcp.tools.StatusTool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrinityMcpServerTest {
    @Test
    void exposesLiveStatusOverStreamableHttp() throws Exception {
        TrinityStatus expected = new TrinityStatus("test-version",
                new TrinityStatus.ProjectStatus(
                        "sample", "C:/sample.tdb", true, null, 100, 12, 3, 4));
        TestTrinityApplication application = new TestTrinityApplication(expected);

        try (TrinityMcpServer server = new TrinityMcpServer(application, "127.0.0.1", 0)) {
            server.start();
            var transport = HttpClientStreamableHttpTransport
                    .builder("http://127.0.0.1:" + server.port())
                    .endpoint(TrinityMcpServer.ENDPOINT)
                    .build();
            var client = McpClient.sync(transport).build();
            try {
                client.initialize();

                List<McpSchema.Tool> listedTools = client.listTools().tools();
                Set<String> tools = listedTools.stream()
                        .map(McpSchema.Tool::name).collect(java.util.stream.Collectors.toSet());
                assertEquals(Set.of(
                        "trinity_status",
                        "project_create", "project_open", "project_save", "project_close",
                        "project_export_jar", "project_get_tree", "project_search",
                        "class_get", "class_get_structure", "class_decompile",
                        "method_get", "method_decompile", "method_get_bytecode",
                        "field_get", "resource_read", "class_get_hierarchy",
                        "xref_find_class", "xref_find_member", "constant_search",
                        "pattern_validate", "pattern_search", "invocation_get_details",
                        "dex_files", "dex_classes", "dex_class_get", "dex_class_disassemble",
                        "dex_class_decompile", "dex_method_get", "dex_method_disassemble",
                        "dex_method_decompile", "dex_find_references", "dex_constant_search",
                        "dex_class_validate_smali", "dex_class_replace_smali",
                        "dex_method_validate_smali", "dex_method_replace_smali",
                        "name_set", "name_revert", "resource_create", "resource_delete",
                        "method_validate_bytecode", "method_replace_bytecode",
                        "refactor_preview", "refactor_apply"), tools);
                assertSearchFields(listedTools, "constant_search");
                assertSearchFields(listedTools, "dex_constant_search");
                assertSearchFields(listedTools, "project_search");
                assertSearchFields(listedTools, "dex_classes");

                assertSuccessful(client.callTool(McpSchema.CallToolRequest.builder("constant_search")
                        .arguments(Map.of("value", "Trinity", "exact", false,
                                "case_sensitive", true)).build()));
                assertFalse(application.lastConstantQuery().exact());
                assertTrue(application.lastConstantQuery().caseSensitive());

                assertSuccessful(client.callTool(McpSchema.CallToolRequest.builder("dex_constant_search")
                        .arguments(Map.of("value", "Trinity", "exact", false,
                                "case_sensitive", true)).build()));
                assertFalse(application.lastDexConstantQuery().exact());
                assertTrue(application.lastDexConstantQuery().caseSensitive());

                assertSuccessful(client.callTool(McpSchema.CallToolRequest.builder("project_search")
                        .arguments(Map.of("query", "sample/Main", "exact", true,
                                "case_sensitive", true)).build()));
                assertTrue(application.lastProjectSearchQuery().exact());
                assertTrue(application.lastProjectSearchQuery().caseSensitive());

                assertSuccessful(client.callTool(McpSchema.CallToolRequest.builder("dex_classes")
                        .arguments(Map.of("query", "sample/DexMain", "exact", true,
                                "case_sensitive", true)).build()));
                assertTrue(application.lastDexClassQuery().exact());
                assertTrue(application.lastDexClassQuery().caseSensitive());

                McpSchema.CallToolResult result = client.callTool(
                        McpSchema.CallToolRequest.builder(StatusTool.NAME).build());
                assertFalse(Boolean.TRUE.equals(result.isError()));
                Map<?, ?> status = (Map<?, ?>) result.structuredContent();
                assertEquals("test-version", status.get("version"));
                Map<?, ?> project = (Map<?, ?>) status.get("project");
                assertEquals("sample", project.get("name"));
                assertEquals(12, ((Number) project.get("classCount")).intValue());

                McpSchema.CallToolResult classResult = client.callTool(
                        McpSchema.CallToolRequest.builder("class_get")
                                .arguments(Map.of("internalName", "sample/Main")).build());
                assertFalse(Boolean.TRUE.equals(classResult.isError()));
                assertEquals("sample/Main",
                        ((Map<?, ?>) classResult.structuredContent()).get("internalName"));

                McpSchema.CallToolResult dexResult = client.callTool(
                        McpSchema.CallToolRequest.builder("dex_class_get")
                                .arguments(Map.of("internalName", "sample/DexMain")).build());
                assertFalse(Boolean.TRUE.equals(dexResult.isError()));
                Map<?, ?> dexClass = (Map<?, ?>) ((Map<?, ?>) dexResult.structuredContent()).get("classInfo");
                assertEquals("sample/DexMain", dexClass.get("internalName"));

                McpSchema.CallToolResult dexJavaResult = client.callTool(
                        McpSchema.CallToolRequest.builder("dex_class_decompile")
                                .arguments(Map.of("internalName", "sample/DexMain")).build());
                assertFalse(Boolean.TRUE.equals(dexJavaResult.isError()));
                Map<?, ?> dexJava = (Map<?, ?>) dexJavaResult.structuredContent();
                assertEquals("java-jadx-1.5.6", dexJava.get("format"));
                assertTrue(((String) dexJava.get("source")).contains("class DexMain"));

                McpSchema.CallToolResult dexValidation = client.callTool(
                        McpSchema.CallToolRequest.builder("dex_method_validate_smali")
                                .arguments(Map.of(
                                        "owner", "sample/DexMain",
                                        "name", "run",
                                        "descriptor", "()V",
                                        "smali", ".method public run()V\n.end method\n",
                                        "expectedRevision", 7)).build());
                assertFalse(Boolean.TRUE.equals(dexValidation.isError()));
                assertEquals(true, ((Map<?, ?>) dexValidation.structuredContent()).get("valid"));

                McpSchema.CallToolResult patternResult = client.callTool(
                        McpSchema.CallToolRequest.builder("pattern_validate")
                                .arguments(Map.of("pattern", "return")).build());
                assertFalse(Boolean.TRUE.equals(patternResult.isError()));
                assertEquals(true, ((Map<?, ?>) patternResult.structuredContent()).get("valid"));

                McpSchema.CallToolResult mutationResult = client.callTool(
                        McpSchema.CallToolRequest.builder("name_set").arguments(Map.of(
                                "kind", "class", "owner", "sample/Main",
                                "newName", "ReadableMain", "expectedRevision", 7)).build());
                assertFalse(Boolean.TRUE.equals(mutationResult.isError()));
                assertEquals(8, ((Number) ((Map<?, ?>) mutationResult.structuredContent())
                        .get("revision")).intValue());

                McpSchema.CallToolResult invalidResult = client.callTool(
                        McpSchema.CallToolRequest.builder("class_get")
                                .arguments(Map.of("internalName", "")).build());
                assertTrue(Boolean.TRUE.equals(invalidResult.isError()));
                String errorText = ((McpSchema.TextContent) invalidResult.content().get(0)).text();
                assertTrue(errorText.contains("INVALID_INPUT"));
            } finally {
                client.closeGracefully();
            }
        }
    }

    @Test
    void rejectsNonLoopbackBindings() {
        TrinityApplication application = new TestTrinityApplication(
                new TrinityStatus("test", null));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new TrinityMcpServer(application, "0.0.0.0", 7331));
    }

    @SuppressWarnings("unchecked")
    private static void assertSearchFields(List<McpSchema.Tool> tools, String name) {
        McpSchema.Tool tool = tools.stream().filter(candidate -> candidate.name().equals(name))
                .findFirst().orElseThrow();
        Map<String, Object> properties =
                (Map<String, Object>) tool.inputSchema().get("properties");
        assertTrue(properties.containsKey("exact"), name + " should expose exact");
        assertTrue(properties.containsKey("case_sensitive"),
                name + " should expose case_sensitive");
        assertFalse(properties.containsKey("caseSensitive"),
                name + " should not expose the Java-style field name");
    }

    private static void assertSuccessful(McpSchema.CallToolResult result) {
        assertFalse(Boolean.TRUE.equals(result.isError()));
    }
}
