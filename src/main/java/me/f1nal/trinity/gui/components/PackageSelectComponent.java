package me.f1nal.trinity.gui.components;

import imgui.ImGui;
import imgui.type.ImString;

public class PackageSelectComponent {
    private final ImString packageText = new ImString(0x200);
    private final String componentId = ComponentId.getId(this.getClass());

    public PackageSelectComponent(String defaultPath) {
        this.packageText.set(defaultPath);
    }

    public void draw() {
        ImGui.inputTextWithHint("###" + this.componentId, "com/example/package/", this.packageText);
    }

    public String getPackagePath() {
        String packageText = this.packageText.get();
        packageText = packageText.replace('\\', '/');
        while (packageText.contains("//")) packageText = packageText.replace("//", "/");
        if (!packageText.endsWith("/")) packageText += "/";
        return packageText;
    }

    public String getClassInPackage(String className) {
        return this.getPackagePath().concat(className);
    }
}
