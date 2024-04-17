package me.f1nal.trinity.util;

import imgui.type.ImString;

public class StringUtil {
    public static String wrapString(String string, int charWrap) {
        int lastBreak = 0;
        int nextBreak = charWrap;
        if (string.length() > charWrap) {
            StringBuilder setString = new StringBuilder();
            do {
                while (string.charAt(nextBreak) != ' ' && nextBreak > lastBreak) {
                    nextBreak--;
                }
                if (nextBreak == lastBreak) {
                    nextBreak = lastBreak + charWrap;
                }
                setString.append(string.substring(lastBreak, nextBreak).trim()).append("\n");
                lastBreak = nextBreak;
                nextBreak += charWrap;

            } while (nextBreak < string.length());
            setString.append(string.substring(lastBreak).trim());
            return setString.toString();
        } else {
            return string;
        }
    }

    public static String limitStringLength(String text, int length) {
        return text.length() <= length ? text : text.substring(0, length);
    }

    public static String capitalizeFirstLetter(String text) {
        if (text.length() <= 1) return text.toUpperCase();
        String lowerCase = text.toLowerCase();
        return Character.toUpperCase(lowerCase.charAt(0)) + lowerCase.substring(1);
    }

    public static ImString wrapImString(String string) {
        dummyImString.set(string, true);
        return dummyImString;
    }

    private static final ImString dummyImString = new ImString();

    // From Fernflower
    public static String convertStringToJava(String value, boolean ascii) {
        char[] arr = value.toCharArray();
        StringBuilder buffer = new StringBuilder(arr.length);

        for (char c : arr) {
            switch (c) {
                case '\\' -> //  u005c: backslash \
                        buffer.append("\\\\");
                case 0x8 -> // "\\\\b");  //  u0008: backspace BS
                        buffer.append("\\b");
                case 0x9 -> //"\\\\t");  //  u0009: horizontal tab HT
                        buffer.append("\\t");
                case 0xA -> //"\\\\n");  //  u000a: linefeed LF
                        buffer.append("\\n");
                case 0xC -> //"\\\\f");  //  u000c: form feed FF
                        buffer.append("\\f");
                case 0xD -> //"\\\\r");  //  u000d: carriage return CR
                        buffer.append("\\r");
//                case 0x22 -> //"\\\\\""); // u0022: double quote "
//                        buffer.append("\\\"");

                default -> {
                    if (isPrintableAscii(c) || !ascii && isPrintableUnicode(c)) {
                        buffer.append(c);
                    }
                    else {
                        buffer.append(charToUnicodeLiteral(c));
                    }
                }
            }
        }

        return buffer.toString();
    }

    public static String charToUnicodeLiteral(int value) {
        String sTemp = Integer.toHexString(value);
        sTemp = ("0000" + sTemp).substring(sTemp.length());
        return "\\u" + sTemp;
    }

    private static boolean isPrintableAscii(int c) {
        return c >= 32 && c < 127;
    }

    public static boolean isPrintableUnicode(char c) {
        int t = Character.getType(c);
        return t != Character.UNASSIGNED && t != Character.LINE_SEPARATOR && t != Character.PARAGRAPH_SEPARATOR &&
                t != Character.CONTROL && t != Character.FORMAT && t != Character.PRIVATE_USE && t != Character.SURROGATE;
    }
}
