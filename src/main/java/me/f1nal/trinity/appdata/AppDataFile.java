package me.f1nal.trinity.appdata;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import me.f1nal.trinity.util.SerializationUtil;

import java.io.File;

public abstract class AppDataFile {
    @XStreamOmitField
    private final String name;
    @XStreamOmitField
    private final XStream stream = new XStream();
    @XStreamOmitField
    private final AppDataManager manager;
    @XStreamOmitField
    private Integer fileHash;
    @XStreamOmitField
    private File file;

    protected AppDataFile(String name, AppDataManager manager) {
        this.name = name;
        this.manager = manager;
        this.addAlias(this.getClass(), name);
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public Integer getFileHash() {
        return fileHash;
    }

    public void setFileHash(Integer fileHash) {
        this.fileHash = fileHash;
    }

    public String serialize() {
        return stream.toXML(this);
    }

    protected void addAlias(Class<?> clazz, String alias) {
        SerializationUtil.addAlias(stream, clazz, alias);
    }

    public XStream getStream() {
        return stream;
    }

    public final String getName() {
        return name;
    }

    public void handleLoad() {
        // May be overridden
    }
}
