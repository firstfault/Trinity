package me.f1nal.trinity.util;

import java.security.SecureRandom;
import java.util.Random;

public final class RandomUtil {
    private static final Random random = new SecureRandom();

    /**
     * Generates a random long with all 64 bits included.
     * @return A randomly generated long.
     */
    public static long getLong() {
        return getNumber(8);
    }

    public static int getInteger() {
        return random.nextInt();
    }

    public static long getNumber(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return ByteUtil.getNumber(bytes, 0, length);
    }

    public static Random getRandom() {
        return random;
    }
}
