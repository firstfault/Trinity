package me.f1nal.trinity.util;

import java.util.regex.Pattern;

public class PatternUtil {
    public static final Pattern LETTERS_ONLY = Pattern.compile("[a-zA-Z]+");
    public static final Pattern DATABASE_NAME = Pattern.compile("[a-zA-Z0-9 _+-]*");
}
