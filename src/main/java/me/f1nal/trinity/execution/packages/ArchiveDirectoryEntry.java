package me.f1nal.trinity.execution.packages;

/** Empty directory records are retained for faithful ZIP exports. */
public final class ArchiveDirectoryEntry {
    private final String name;
    private ZipEntryMetadata metadata;

    public ArchiveDirectoryEntry(String name, ZipEntryMetadata metadata) {
        this.name = name.endsWith("/") ? name : name + "/";
        this.metadata = metadata == null ? ZipEntryMetadata.createDefault() : metadata;
    }

    public String getName() {
        return name;
    }

    public ZipEntryMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ZipEntryMetadata metadata) {
        this.metadata = metadata;
    }
}
