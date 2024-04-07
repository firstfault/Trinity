package me.f1nal.trinity.util;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.util.Printer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class NameUtil {
    private final static String[] wordList;

    private final static Random random = new SecureRandom();

    public static String cleanNewlines(String text) {
        return text.replace("\n", "\\n");
    }

    /**
     * Generates any random word.
     * @return A random word from {@link NameUtil#wordList}
     */
    public static String generateWord() {
        return wordList[random.nextInt(wordList.length)];
    }

    public static String internalToNormal(String internalName) {
        return internalName.replace('.', '/');
    }

    /**
     * Substrings a class name to the last '/' character.
     * @return Simple class name.
     */
    public static String getSimpleName(String name) {
        return name.substring(name.lastIndexOf('/') + 1);
    }

    public static String getExtension(String fileName) {
        int indexOf = fileName.lastIndexOf('.');
        return indexOf == -1 ? null : fileName.substring(indexOf + 1);
    }

    public static String removeExtensions(String fileName) {
        int indexOf = fileName.indexOf('.');
        return indexOf == -1 ? fileName : fileName.substring(0, indexOf);
    }

    private static int uniqueWordSequenceIndex, uniqueWordSequenceDepth;

    /**
     * Generates a unique random word sequence.
     * <p>
     *     This method will never return the same {@link String} twice. If the word list has been used, words will be chained together until the result is unique.
     * </p>
     * @return A unique sequence of words.
     */
    public static @NotNull String generateUniqueWords() {
        // TODO
        return generateWord();
    }

    /**
     * Generates a random sequence of uppercase letters, lowercase letters, and numbers.
     *
     * @param length The length of the random sequence to generate.
     * @return The generated random sequence.
     */
    public static String generateRandomSequence(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be a positive integer");
        }

        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder randomSequence = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            randomSequence.append(randomChar);
        }

        return randomSequence.toString();
    }

    public static String getOpcodeName(int opcode) {
        if (opcode < 0 || opcode >= Printer.OPCODES.length) {
            return java.lang.String.valueOf(opcode);
        }
        return Printer.OPCODES[opcode];
    }

    public static String[] getWordList() {
        return wordList;
    }

    static {
        final InputStream stream = NameUtil.class.getClassLoader().getResourceAsStream("wordlist");
        if (stream == null) {
            throw new RuntimeException("No wordlist available.");
        }
        try {
            final List<String> wordArrayList = new ArrayList<>();
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (PatternUtil.LETTERS_ONLY.matcher(line).matches()) {
                    wordArrayList.add(line);
                }
            }
            stream.close();
            Collections.shuffle(wordArrayList, random);
            wordList = wordArrayList.toArray(new String[0]);
        } catch (IOException e) {
            throw new RuntimeException("Failed to handle wordlist resource", e);
        }
        if (wordList.length < 1024) {
            throw new RuntimeException("Not enough words! " + wordList.length);
        }
    }
}
