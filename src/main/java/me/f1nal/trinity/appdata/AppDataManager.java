package me.f1nal.trinity.appdata;

import com.google.common.io.Files;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.logging.Logging;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.theme.Theme;
import me.f1nal.trinity.theme.ThemeColor;
import me.f1nal.trinity.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Controller for everything related to managing settings saved on disk (excluding databases).
 */
public class AppDataManager {
    private final PreferencesFile preferencesFile = new PreferencesFile(this);
    private final RecentDatabasesFile recentDatabasesFile = new RecentDatabasesFile(this);
    private final StateFile stateFile = new StateFile(this);
    private final List<AppDataFile> files = List.of(preferencesFile, recentDatabasesFile, stateFile);
    private final File directory, themesDirectory;

    public AppDataManager() {
        this.directory = new File(getDefaultDirectory(), ".trinity");
        this.directory.mkdirs();
        this.themesDirectory = new File(this.directory, "themes");
        this.themesDirectory.mkdirs();
    }

    public void load() {
        this.reloadThemes();
        for (AppDataFile file : files) {
            file.setFile(new File(this.directory, file.getName() + ".xml"));
            this.loadFromFile(file);
        }
        Main.getScheduler().scheduleAtFixedRate(this::scheduledSave, 30L, 30L, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(this::scheduledSave, "AppData Save"));
    }

    public File getThemeFile(Theme theme) {
        return new File(this.themesDirectory, FileUtil.normalizeFileName(theme.getName().replace(' ', '_')) + ".theme");
    }

    public boolean saveTheme(Theme theme) {
        return this.saveTheme(theme, this.getThemeFile(theme));
    }

    public boolean saveTheme(Theme theme, File file) {
        try {
            ThemeFile themeFile = new ThemeFile();
            for (ThemeColor color : theme.getColors()) {
                themeFile.getColors().put(color.getLabel(), CodeColorScheme.getRgb(color.getRgba()));
            }
            byte[] bytes = ThemeFile.serialize(themeFile).getBytes();
            Files.write(bytes, file);
            return true;
        } catch (Throwable e) {
            Logging.error("Failed to save theme file '{}' due to: {}", theme.getName(), e);
            return false;
        }
    }

    public void reloadThemes() {
        final File[] themeFiles = this.themesDirectory.listFiles();
        if (themeFiles == null) {
            return;
        }
        Main.getThemeManager().clearThemes();
        for (File file : themeFiles) {
            if (!file.isFile()) {
                continue;
            }
            final String fileName = file.getName();
            if (!fileName.endsWith(".theme")) {
                continue;
            }
            final String themeName = this.getThemeName(fileName);
            if (themeName.isEmpty()) {
                continue;
            }
            this.loadTheme(themeName, file);
        }
    }

    public String getThemeName(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf(".theme")).replace('_', ' ');
    }

    public Theme loadTheme(String themeName, File file) {
        try {
            byte[] bytes = Files.toByteArray(file);
            ThemeFile themeFile = new ThemeFile();
            ThemeFile.deserialize(themeFile, new String(bytes));
            Theme theme = new Theme(themeName, true);
            theme.readFrom(themeFile);
            if (!Main.getThemeManager().addTheme(theme)) {
                throw new IOException("Unable to add theme to manager");
            }
            return theme;
        } catch (Throwable e) {
            Logging.warn("Failed to load theme {} due to: {}", themeName, e);
        }
        return null;
    }

    public File getThemesDirectory() {
        return themesDirectory;
    }

    public File getDirectory() {
        return directory;
    }

    private void loadFromFile(AppDataFile file) {
        try {
            if (file.getFile().exists()) {
                byte[] bytes = Files.toByteArray(file.getFile());
                String xml = new String(bytes);
                file.setFileHash(xml.hashCode());
                file.getStream().fromXML(xml, file);
                file.handleLoad();
                return;
            }
        } catch (Throwable throwable) {
            Logging.error("Failed to load AppData file {}: {}", file.getName(), throwable);
        }
        file.handleLoad();
    }

    private static String getDefaultDirectory() {
        String os = System.getProperty("os.name").toUpperCase();
        if (os.contains("WIN")) {
            String appdata = System.getenv("APPDATA");
            if (appdata != null) {
                return appdata;
                // otherwise fall back to default
            }
        } else if (os.contains("MAC"))
            return System.getProperty("user.home") + "/Library/Application " + "Support";
        else if (os.contains("NUX"))
            return System.getProperty("user.home");
        return Objects.requireNonNullElse(System.getProperty("user.dir"), System.getProperty("user.home"));
    }

    private void scheduledSave() {
        for (AppDataFile file : files) {
            String serialize = file.serialize();

            if (file.getFileHash() == null || file.getFileHash() != serialize.hashCode()) {
                if (!this.saveFile(file.getFile(), serialize)) {
                    Logging.warn("Failed to save file {}", file.getName());
                } else {
                    file.setFileHash(serialize.hashCode());
                }
            }
        }
    }

    public RecentDatabasesFile getRecentDatabases() {
        return recentDatabasesFile;
    }

    public StateFile getState() {
        return stateFile;
    }

    private boolean saveFile(File file, String serialize) {
        try {
            Files.write(serialize.getBytes(), file);
            return true;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return false;
        }
    }

    public PreferencesFile getPreferences() {
        return preferencesFile;
    }
}
