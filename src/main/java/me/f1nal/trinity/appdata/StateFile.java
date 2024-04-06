package me.f1nal.trinity.appdata;

public class StateFile extends AppDataFile {
    private String lastLaunchedVersion;
    private boolean databaseLoaded;
//    private GuiStateMemory guiStateMemory;

    protected StateFile(AppDataManager manager) {
        super("state", manager);
    }

//    @Override
//    public void handleLoad() {
//        if (guiStateMemory == null) guiStateMemory = new GuiStateMemory();
//    }

    public void setDatabaseLoaded(boolean databaseLoaded) {
        this.databaseLoaded = databaseLoaded;
    }

    public String getLastLaunchedVersion() {
        return lastLaunchedVersion;
    }

    public boolean isDatabaseLoaded() {
        return databaseLoaded;
    }

    public void setLastLaunchedVersion(String lastLaunchedVersion) {
        this.lastLaunchedVersion = lastLaunchedVersion;
    }
}
