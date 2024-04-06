package me.f1nal.trinity.util;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public class ByteUtil {
    public static long getNumber(byte[] bytes, int offset, int size) {
        long number = 0;
        int shift = (size - 1) * 8;

        for (int i = 0; i < size; i++) {
            number |= (long) (bytes[offset + i] & 0xFF) << shift;
            shift -= 8;
        }

        return number;
    }

    public static byte[] getBytes(long number, int size) {
        byte[] bytes = new byte[size];
        int shift = (size - 1) * 8;

        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) ((number >> shift) & 0xFF);
            shift -= 8;
        }

        return bytes;
    }

    public static String getHumanReadableByteCountSI(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }
}
