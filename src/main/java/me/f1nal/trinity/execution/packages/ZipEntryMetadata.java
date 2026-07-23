package me.f1nal.trinity.execution.packages;

import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.zip.ZipEntry;

/** ZIP metadata retained independently from an entry's uncompressed payload. */
public final class ZipEntryMetadata {
    public static final long MISSING_TIME = Long.MIN_VALUE;

    private int order;
    private int method;
    private long modifiedTime;
    private long accessTime;
    private long creationTime;
    private String comment;
    private byte[] extra;
    private long crc;
    private long compressedSize;

    public ZipEntryMetadata(int order, int method, long modifiedTime, long accessTime,
                            long creationTime, String comment, byte[] extra,
                            long crc, long compressedSize) {
        this.order = order;
        this.method = normalizeMethod(method);
        this.modifiedTime = modifiedTime;
        this.accessTime = accessTime;
        this.creationTime = creationTime;
        this.comment = comment;
        this.extra = extra == null ? null : extra.clone();
        this.crc = crc;
        this.compressedSize = compressedSize;
    }

    public static ZipEntryMetadata fromZipEntry(ZipEntry entry, int order) {
        return new ZipEntryMetadata(order, entry.getMethod(),
                time(entry.getLastModifiedTime()), time(entry.getLastAccessTime()),
                time(entry.getCreationTime()), entry.getComment(), entry.getExtra(),
                entry.getCrc(), entry.getCompressedSize());
    }

    public static ZipEntryMetadata createDefault() {
        return new ZipEntryMetadata(Integer.MAX_VALUE, ZipEntry.DEFLATED,
                System.currentTimeMillis(), MISSING_TIME, MISSING_TIME, null, null, -1L, -1L);
    }

    public ZipEntryMetadata copy() {
        return new ZipEntryMetadata(order, method, modifiedTime, accessTime, creationTime,
                comment, extra, crc, compressedSize);
    }

    private static long time(FileTime time) {
        return time == null ? MISSING_TIME : time.toMillis();
    }

    private static int normalizeMethod(int method) {
        return method == ZipEntry.STORED ? ZipEntry.STORED : ZipEntry.DEFLATED;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getMethod() {
        return method;
    }

    public void setMethod(int method) {
        this.method = normalizeMethod(method);
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public long getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(long accessTime) {
        this.accessTime = accessTime;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment == null || comment.isEmpty() ? null : comment;
    }

    public byte[] getExtra() {
        return extra == null ? null : extra.clone();
    }

    public void setExtra(byte[] extra) {
        this.extra = extra == null || extra.length == 0 ? null : extra.clone();
    }

    public long getCrc() {
        return crc;
    }

    public void setCrc(long crc) {
        this.crc = crc;
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    public void setCompressedSize(long compressedSize) {
        this.compressedSize = compressedSize;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ZipEntryMetadata other)) return false;
        return order == other.order && method == other.method
                && modifiedTime == other.modifiedTime && accessTime == other.accessTime
                && creationTime == other.creationTime && crc == other.crc
                && compressedSize == other.compressedSize
                && java.util.Objects.equals(comment, other.comment)
                && Arrays.equals(extra, other.extra);
    }

    @Override
    public int hashCode() {
        int result = java.util.Objects.hash(order, method, modifiedTime, accessTime,
                creationTime, comment, crc, compressedSize);
        return 31 * result + Arrays.hashCode(extra);
    }
}
