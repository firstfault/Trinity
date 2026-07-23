package me.f1nal.trinity.gui.windows.impl.project;

import imgui.ImGui;
import imgui.flag.ImGuiFocusedFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;
import imgui.type.ImString;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.events.EventPackageStructureReload;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.packages.ArchiveDirectoryEntry;
import me.f1nal.trinity.execution.packages.ProjectContainer;
import me.f1nal.trinity.execution.packages.ResourceArchiveEntry;
import me.f1nal.trinity.execution.packages.ZipEntryMetadata;
import me.f1nal.trinity.gui.components.CodiconIcons;
import me.f1nal.trinity.gui.components.IconFamily;
import me.f1nal.trinity.gui.windows.api.ClosableWindow;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.ByteUtil;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;

/** Edits archive- and central-directory metadata without rebuilding class payloads. */
public final class EditJarWindow extends ClosableWindow {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int ZIP_TEXT_LIMIT = 65_535;

    private final ProjectContainer container;
    private final ImString archiveName;
    private final ImString archiveComment;
    private final ImString filter = new ImString(512);
    private final List<EntryDraft> entries = new ArrayList<>();
    private final ImInt entryOrder = new ImInt();
    private final ImString entryComment = new ImString(70_000);
    private final ImString entryModified = new ImString(64);
    private final ImString entryAccessed = new ImString(64);
    private final ImString entryCreated = new ImString(64);
    private final ImString entryExtra = new ImString(140_000);
    private EntryDraft selection;
    private boolean archiveSelected = true;
    private boolean dirty;
    private String error;

    public EditJarWindow(Trinity trinity, ProjectContainer container) {
        super("Edit JAR - " + container.getName(), 820.F, 560.F, trinity);
        if (!container.isJar()) throw new IllegalArgumentException("Only JAR containers have ZIP metadata");
        this.container = container;
        this.archiveName = text(container.getName(), 4_096);
        this.archiveComment = text(container.getArchiveComment(), 70_000);
        container.getClasses().forEach(target -> entries.add(EntryDraft.forClass(target)));
        container.getResources().forEach(resource -> entries.add(EntryDraft.forResource(resource)));
        container.getDirectories().forEach(directory -> entries.add(EntryDraft.forDirectory(directory)));
        entries.sort(Comparator.comparingInt(entry -> entry.metadata.getOrder()));
        this.setDialog(true);
    }

    @Override
    public void render() {
        if (dirty) windowFlags |= ImGuiWindowFlags.UnsavedDocument;
        else windowFlags &= ~ImGuiWindowFlags.UnsavedDocument;
        super.render();
    }

    @Override
    protected void renderFrame() {
        drawHeader();
        ImGui.separator();

        float leftWidth = Math.min(315.F, ImGui.getContentRegionAvailX() * 0.39F);
        if (ImGui.beginChild(getId("EntryList"), leftWidth, 0.F, false)) drawEntryList();
        ImGui.endChild();
        ImGui.sameLine();
        if (ImGui.beginChild(getId("MetadataEditor"), 0.F, 0.F, false)) {
            if (archiveSelected) drawArchiveEditor();
            else if (selection != null) drawEntryEditor(selection);
        }
        ImGui.endChild();

        if (ImGui.isWindowFocused(ImGuiFocusedFlags.RootAndChildWindows)
                && ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(ImGuiKey.S, false)) {
            save();
        }
    }

    private void drawHeader() {
        drawIcon(CodiconIcons.ARCHIVE, CodeColorScheme.ARCHIVE_REF);
        ImGui.sameLine();
        ImGui.text(container.getName());
        ImGui.sameLine();
        ImGui.textDisabled(String.format(Locale.ROOT, "%d classes  /  %d resources  /  %d entries",
                container.getClasses().size(), container.getResources().size(), entries.size()));
        if (error != null) {
            ImGui.sameLine();
            ImGui.textColored(CodeColorScheme.NOTIFY_ERROR, error);
        }
    }

    private void drawEntryList() {
        ImGui.pushItemWidth(-1.F);
        ImGui.inputTextWithHint("###JarEntryFilter", "Filter entries...", filter);
        ImGui.popItemWidth();
        drawIcon(CodiconIcons.ARCHIVE, CodeColorScheme.ARCHIVE_REF);
        boolean archiveIconClicked = ImGui.isItemClicked(0);
        ImGui.sameLine(0.F, 5.F);
        boolean archiveLabelClicked = ImGui.selectable("Archive settings###ArchiveSettings", archiveSelected);
        if (archiveIconClicked || archiveLabelClicked) {
            archiveSelected = true;
            selection = null;
        }
        ImGui.separator();

        String term = filter.get().toLowerCase(Locale.ROOT);
        for (EntryDraft entry : entries) {
            if (!term.isEmpty() && !entry.path.toLowerCase(Locale.ROOT).contains(term)) continue;
            drawIcon(entry.icon, entry.color);
            boolean iconHovered = ImGui.isItemHovered();
            boolean iconClicked = ImGui.isItemClicked(0);
            ImGui.sameLine(0.F, 5.F);
            String label = entry.path + "###JarEntry" + entry.id;
            boolean labelClicked = ImGui.selectable(label, selection == entry && !archiveSelected);
            if (iconClicked || labelClicked) {
                selectEntry(entry);
            }
            if (iconHovered || ImGui.isItemHovered()) {
                ImGui.setTooltip(entry.path + "\n" + entry.kind + "  /  "
                        + ByteUtil.getHumanReadableByteCountSI(entry.size));
            }
        }
    }

    private static void drawIcon(String icon, int color) {
        IconFamily.CODICON.pushFont();
        ImGui.textColored(color, icon);
        IconFamily.CODICON.popFont();
    }

    private void selectEntry(EntryDraft entry) {
        selection = entry;
        archiveSelected = false;
        entryOrder.set(entry.order);
        entryComment.set(entry.comment);
        entryModified.set(entry.modified);
        entryAccessed.set(entry.accessed);
        entryCreated.set(entry.created);
        if (entry.extra == null) {
            byte[] bytes = entry.metadata.getExtra();
            entry.extra = bytes == null ? "" : HexFormat.of().withUpperCase().formatHex(bytes);
        }
        entryExtra.set(entry.extra);
    }

    private void drawArchiveEditor() {
        ImGui.separatorText("Archive");
        ImGui.textDisabled("The display and default export name used by this project.");
        ImGui.setNextItemWidth(-1.F);
        if (ImGui.inputText("Name", archiveName)) changed();

        ImGui.spacing();
        ImGui.separatorText("ZIP comment");
        ImGui.textDisabled("Stored in the ZIP end-of-central-directory record.");
        if (ImGui.inputTextMultiline("###ArchiveComment", archiveComment, -1.F, 150.F)) changed();

        ImGui.spacing();
        ImGui.textDisabled("Ctrl+S applies metadata changes. Class and resource payloads are not modified.");
    }

    private void drawEntryEditor(EntryDraft entry) {
        ImGui.separatorText(entry.kind);
        ImGui.textWrapped(entry.path);
        ImGui.textDisabled(ByteUtil.getHumanReadableByteCountSI(entry.size)
                + (entry.metadata.getCompressedSize() >= 0
                ? " uncompressed  /  " + ByteUtil.getHumanReadableByteCountSI(entry.metadata.getCompressedSize()) + " stored" : ""));

        ImGui.spacing();
        ImGui.separatorText("Storage");
        String method = entry.metadata.getMethod() == ZipEntry.STORED ? "Stored (no compression)" : "Deflated";
        ImGui.setNextItemWidth(-1.F);
        if (ImGui.beginCombo("Compression", method)) {
            if (ImGui.selectable("Deflated", entry.metadata.getMethod() == ZipEntry.DEFLATED)) {
                entry.metadata.setMethod(ZipEntry.DEFLATED);
                changed();
            }
            if (ImGui.selectable("Stored (no compression)", entry.metadata.getMethod() == ZipEntry.STORED)) {
                entry.metadata.setMethod(ZipEntry.STORED);
                changed();
            }
            ImGui.endCombo();
        }
        if (ImGui.inputInt("Entry order", entryOrder)) {
            entry.order = Math.max(0, entryOrder.get());
            entryOrder.set(entry.order);
            changed();
        }

        ImGui.spacing();
        ImGui.separatorText("Timestamps");
        if (drawTimestamp("Modified", entryModified)) entry.modified = entryModified.get();
        if (drawTimestamp("Accessed", entryAccessed)) entry.accessed = entryAccessed.get();
        if (drawTimestamp("Created", entryCreated)) entry.created = entryCreated.get();
        if (ImGui.isItemHovered()) ImGui.setTooltip("Local time: yyyy-MM-dd HH:mm:ss. Leave blank to use the ZIP writer default.");

        ImGui.spacing();
        ImGui.separatorText("Entry comment");
        if (ImGui.inputTextMultiline("###EntryComment" + entry.id, entryComment, -1.F, 76.F)) {
            entry.comment = entryComment.get();
            changed();
        }

        ImGui.spacing();
        ImGui.separatorText("Extra field");
        ImGui.textDisabled("Raw ZIP extra-field bytes in hexadecimal.");
        if (ImGui.inputTextMultiline("###EntryExtra" + entry.id, entryExtra, -1.F, 70.F)) {
            entry.extra = entryExtra.get();
            changed();
        }
    }

    private boolean drawTimestamp(String label, ImString value) {
        ImGui.setNextItemWidth(-1.F);
        if (!ImGui.inputText(label, value)) return false;
        changed();
        return true;
    }

    private void changed() {
        dirty = true;
        error = null;
    }

    private void save() {
        try {
            String name = archiveName.get().trim();
            if (name.isEmpty()) throw new IllegalArgumentException("Archive name cannot be empty.");
            validateZipText(archiveComment.get(), "Archive comment");
            for (EntryDraft entry : entries) entry.validateAndApplyDraft();

            container.setName(name);
            container.setArchiveComment(emptyToNull(archiveComment.get()));
            for (EntryDraft entry : entries) entry.apply.accept(entry.metadata.copy());
            container.refreshEntryOrderCounter();
            trinity.getEventManager().postEvent(new EventPackageStructureReload());
            dirty = false;
            windowFlags &= ~ImGuiWindowFlags.UnsavedDocument;
            close();
        } catch (IllegalArgumentException exception) {
            error = exception.getMessage();
        }
    }

    private static void validateZipText(String value, String name) {
        if (value.getBytes(StandardCharsets.UTF_8).length > ZIP_TEXT_LIMIT) {
            throw new IllegalArgumentException(name + " exceeds 65,535 UTF-8 bytes.");
        }
    }

    private static ImString text(String value, int capacity) {
        return new ImString(value == null ? "" : value, capacity);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static String formatTime(long millis) {
        if (millis == ZipEntryMetadata.MISSING_TIME) return "";
        return TIME_FORMAT.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()));
    }

    private static long parseTime(String value, String label) {
        if (value.isBlank()) return ZipEntryMetadata.MISSING_TIME;
        try {
            LocalDateTime parsed = LocalDateTime.parse(value.trim(), TIME_FORMAT);
            return parsed.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(label + " timestamp must use yyyy-MM-dd HH:mm:ss.");
        }
    }

    private static byte[] parseHex(String text) {
        String compact = text.replaceAll("\\s+", "");
        if (compact.isEmpty()) return null;
        if ((compact.length() & 1) != 0) throw new IllegalArgumentException("Extra field must contain complete hex bytes.");
        byte[] bytes;
        try {
            bytes = HexFormat.of().parseHex(compact);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Extra field contains invalid hexadecimal text.");
        }
        if (bytes.length > ZIP_TEXT_LIMIT) throw new IllegalArgumentException("Extra field exceeds 65,535 bytes.");
        for (int offset = 0; offset < bytes.length;) {
            if (bytes.length - offset < 4) {
                throw new IllegalArgumentException("Extra field ends before a ZIP field header is complete.");
            }
            int size = Byte.toUnsignedInt(bytes[offset + 2])
                    | Byte.toUnsignedInt(bytes[offset + 3]) << 8;
            offset += 4;
            if (size > bytes.length - offset) {
                throw new IllegalArgumentException("Extra field declares data beyond the available bytes.");
            }
            offset += size;
        }
        return bytes;
    }

    @Override
    public boolean isAlreadyOpen(ClosableWindow otherWindow) {
        return otherWindow instanceof EditJarWindow other && other.container.getId().equals(container.getId());
    }

    private static final class EntryDraft {
        private static int nextId;
        private final int id = ++nextId;
        private final String path;
        private final String kind;
        private final String icon;
        private final int color;
        private final int size;
        private final Consumer<ZipEntryMetadata> apply;
        private final ZipEntryMetadata metadata;
        private int order;
        private String comment;
        private String modified;
        private String accessed;
        private String created;
        private String extra;

        private EntryDraft(String path, String kind, String icon, int color, int size,
                           ZipEntryMetadata metadata, Consumer<ZipEntryMetadata> apply) {
            this.path = path;
            this.kind = kind;
            this.icon = icon;
            this.color = color;
            this.size = size;
            this.apply = apply;
            this.metadata = metadata.copy();
            this.order = metadata.getOrder();
            this.comment = metadata.getComment() == null ? "" : metadata.getComment();
            this.modified = formatTime(metadata.getModifiedTime());
            this.accessed = formatTime(metadata.getAccessTime());
            this.created = formatTime(metadata.getCreationTime());
            this.extra = null;
        }

        private static EntryDraft forClass(ClassTarget target) {
            return new EntryDraft(target.getInput().getExportEntryName(), "Class entry",
                    CodiconIcons.SYMBOL_CLASS, CodeColorScheme.FILE_CLASS, target.getSizeInBytes(),
                    target.getZipMetadata(), target::setZipMetadata);
        }

        private static EntryDraft forResource(ResourceArchiveEntry resource) {
            return new EntryDraft(resource.getRealName(), "Resource entry", CodiconIcons.FILE,
                    CodeColorScheme.FILE_RESOURCE, resource.getSizeInBytes(),
                    resource.getZipMetadata(), resource::setZipMetadata);
        }

        private static EntryDraft forDirectory(ArchiveDirectoryEntry directory) {
            return new EntryDraft(directory.getName(), "Directory entry", CodiconIcons.FOLDER,
                    CodeColorScheme.PACKAGE, 0, directory.getMetadata(), directory::setMetadata);
        }

        private void validateAndApplyDraft() {
            validateZipText(comment, "Entry comment");
            metadata.setOrder(Math.max(0, order));
            metadata.setComment(emptyToNull(comment));
            metadata.setModifiedTime(parseTime(modified, "Modified"));
            metadata.setAccessTime(parseTime(accessed, "Accessed"));
            metadata.setCreationTime(parseTime(created, "Created"));
            if (extra != null) metadata.setExtra(parseHex(extra));
        }
    }
}
