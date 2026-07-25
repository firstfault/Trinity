package me.f1nal.trinity.mcp.tools;

import io.modelcontextprotocol.json.McpJsonMapper;
import me.f1nal.trinity.application.ProjectService;
import me.f1nal.trinity.application.TrinityApplication;

import java.util.List;

import static me.f1nal.trinity.mcp.tools.ToolSupport.*;

/** MCP presentation adapters for project lifecycle, tree, and search use cases. */
public final class ProjectTools {
    private ProjectTools() {
    }

    public static List<IMcpToolAdapter> create(TrinityApplication application, McpJsonMapper mapper) {
        ProjectService projects = application.projects();
        return List.of(
                new JsonToolAdapter("project_create", "Create Trinity Project",
                        "Creates a project from local JAR, ZIP, class, DEX, APK, or APKM inputs and installs it as the active workspace.",
                        object(properties(
                                "name", string("Project name"),
                                "databasePath", string("Output .tdb database path"),
                                "compression", string("Database compression: LZ4, GZIP, LZMA2/XZ, or Raw"),
                                "inputPaths", strings("Local JAR, ZIP, class, DEX, APK, or APKM input paths")),
                                "name", "databasePath", "inputPaths"),
                        false, false, false, true, mapper, args -> projects.create(
                        new ProjectService.CreateProject(requiredString(args, "name"),
                                requiredString(args, "databasePath"),
                                optionalString(args, "compression", "LZ4"), strings(args, "inputPaths")))),
                new JsonToolAdapter("project_open", "Open Trinity Project",
                        "Loads a .tdb database into an empty Trinity workspace.",
                        object(properties("databasePath", string("Existing .tdb database path")),
                                "databasePath"),
                        false, false, false, true, mapper, args -> projects.open(
                        new ProjectService.OpenProject(requiredString(args, "databasePath")))),
                new JsonToolAdapter("project_save", "Save Trinity Project",
                        "Saves the active project when its revision matches expectedRevision.",
                        object(properties("expectedRevision", integer("Required current project revision")),
                                "expectedRevision"),
                        false, false, true, true, mapper,
                        args -> projects.save(requiredLong(args, "expectedRevision"))),
                new JsonToolAdapter("project_close", "Close Trinity Project",
                        "Optionally saves and then closes the active project.",
                        object(properties(
                                "expectedRevision", integer("Required current project revision"),
                                "save", bool("Save before closing; defaults to true")),
                                "expectedRevision"),
                        false, true, false, true, mapper, args -> projects.close(
                        new ProjectService.CloseProject(requiredLong(args, "expectedRevision"),
                                bool(args, "save", true)))),
                new JsonToolAdapter("project_export_jar", "Export Project JAR",
                        "Writes all current classes and resources to a local JAR file.",
                        object(properties(
                                "outputPath", string("Destination JAR path"),
                                "expectedRevision", integer("Required current project revision")),
                                "outputPath", "expectedRevision"),
                        false, false, false, true, mapper, args -> projects.exportJar(
                        new ProjectService.ExportJar(requiredString(args, "outputPath"),
                                requiredLong(args, "expectedRevision")))),
                new JsonToolAdapter("project_get_tree", "Get Project Tree",
                        "Returns a paginated package, class, and resource tree.",
                        object(properties(
                                "prefix", string("Optional slash-separated path prefix"),
                                "kind", string("all, package, class, or resource"),
                                "offset", integer("Zero-based result offset"),
                                "limit", integer("Maximum results, capped at 500"))),
                        true, false, true, false, mapper, args -> projects.tree(
                        new ProjectService.TreeQuery(optionalString(args, "prefix", ""),
                                optionalString(args, "kind", "all"), integer(args, "offset", 0),
                                integer(args, "limit", 100)))),
                new JsonToolAdapter("project_search", "Search Project",
                        "Searches real and display names across packages, classes, methods, fields, and resources.",
                        object(properties(
                                "query", string("Case-insensitive search text; empty returns every target"),
                                "kind", string("all, package, class, method, field, or resource"),
                                "offset", integer("Zero-based result offset"),
                                "limit", integer("Maximum results, capped at 500"))),
                        true, false, true, false, mapper, args -> projects.search(
                        new ProjectService.SearchQuery(optionalString(args, "query", ""),
                                optionalString(args, "kind", "all"), integer(args, "offset", 0),
                                integer(args, "limit", 100)))));
    }
}
