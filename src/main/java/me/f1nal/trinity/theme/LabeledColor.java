package me.f1nal.trinity.theme;

import imgui.app.Color;

import java.awt.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LabeledColor {
    ThemeColorCategory category();
    String label();
}
