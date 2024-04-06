package me.f1nal.trinity.theme;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface LabeledColor {
    ThemeColorCategory category();
    String label();
}
