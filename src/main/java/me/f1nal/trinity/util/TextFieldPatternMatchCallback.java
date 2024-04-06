package me.f1nal.trinity.util;

import imgui.ImGuiInputTextCallbackData;
import imgui.callback.ImGuiInputTextCallback;

import java.util.regex.Pattern;

public class TextFieldPatternMatchCallback extends ImGuiInputTextCallback {
    private final Pattern pattern;

    public TextFieldPatternMatchCallback(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public void accept(ImGuiInputTextCallbackData callbackData) {
        String string = String.valueOf((char) callbackData.getEventChar());
        if (!pattern.matcher(string).matches()) {
            callbackData.setEventChar(0);
        }
    }
}
