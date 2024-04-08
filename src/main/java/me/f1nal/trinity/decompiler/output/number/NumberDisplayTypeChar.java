package me.f1nal.trinity.decompiler.output.number;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

public class NumberDisplayTypeChar extends NumberDisplayType {
    private static final ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[8]);

    @Override
    public String getTextImpl(Number number) {
        byteBuffer.position(0);
        int bytes = 0;
        if (number instanceof Long) {
            bytes = 8;
            byteBuffer.putLong(number.longValue());
        } else if (number instanceof Integer) {
            bytes = 4;
            byteBuffer.putInt(number.intValue());
        } else if (number instanceof Short) {
            bytes = 2;
            byteBuffer.putShort(number.shortValue());
        } else if (number instanceof Byte) {
            bytes = 1;
            byteBuffer.put(number.byteValue());
        }
        boolean skip = true;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes; i++) {
            byte b = byteBuffer.get(i);
            if (skip && b == 0) {
                continue;
            }
            skip = false;
            String escape = escapes.get((char) b);
            if (escape != null) {
                sb.append(escape);
                continue;
            }
            if (b >= 33 && b <= 126) {
                sb.append((char) b);
            } else {
                sb.append("\\").append(b);
            }
        }
        return String.format("'%s'", sb);
    }

    @Override
    public String getLabel() {
        return "ASCII";
    }

    private static final Map<Character, String> escapes = new LinkedHashMap<>();
    
    static {
        escapes.put('\\', "\\\\");
        escapes.put('\t', "\\t");
        escapes.put('\b', "\\b");
        escapes.put('\n', "\\n");
        escapes.put('\r', "\\r");
        escapes.put('\f', "\\f");
        escapes.put('\'', "\\'");
        escapes.put('\"', "\\\"");
    }
}
