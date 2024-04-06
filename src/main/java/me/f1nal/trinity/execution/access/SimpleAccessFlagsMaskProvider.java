package me.f1nal.trinity.execution.access;

public class SimpleAccessFlagsMaskProvider implements AccessFlagsMaskProvider {
    private int accessFlags;

    public SimpleAccessFlagsMaskProvider(int accessFlags) {
        this.setAccessFlagsMask(accessFlags);
    }

    public SimpleAccessFlagsMaskProvider() {
    }

    @Override
    public void setAccessFlagsMask(int accessFlagsMask) {
        this.accessFlags = accessFlagsMask;
    }

    @Override
    public int getAccessFlagsMask() {
        return this.accessFlags;
    }
}
