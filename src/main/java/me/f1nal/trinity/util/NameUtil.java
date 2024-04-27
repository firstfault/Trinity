package me.f1nal.trinity.util;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import org.objectweb.asm.util.Printer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.*;

public class NameUtil {
    /**
     * List of words mapped by their initial character.
     */
    private final static ListMultimap<Character, String> wordMap = Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
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
            return String.valueOf(opcode);
        }
        return Printer.OPCODES[opcode];
    }

    /**
     * Counts the amount of words in this text.
     * @param string Text to analyze.
     * @return Count of words.
     */
    public static TextAnalysisResult getWordAnalysis(String string) {
        final char[] chars = string.toCharArray();
        final int length = chars.length;
        int wordCount = 0, recognizedCount = 0, unrecognizedCount = 0;

        for (int i = 0; i < length; i++) {
            final char c = chars[i];

            List<String> strings = wordMap.get(c);
            String word = getWord(chars, i, strings);

            if (word != null) {
                i += word.length() - 1;
                recognizedCount += word.length();
                ++wordCount;
            } else {
                ++unrecognizedCount;
            }
        }

        return new TextAnalysisResult(wordCount, recognizedCount, unrecognizedCount);
    }

    private static String getWord(char[] chars, int index, List<String> strings) {
        stringLoop:
        for (String string : strings) {
            char[] wordChars = string.toCharArray();

            for (int j = 0; j < wordChars.length; j++) {
                if (j >= chars.length - index || wordChars[j] != chars[index + j]) {
                    continue stringLoop;
                }
            }

            return string;
        }

        return null;
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
                if (line.length() <= (wordArrayList.size() < 200 ? 1 : 2)) {
                    continue;
                }
                if (PatternUtil.LETTERS_ONLY.matcher(line).matches()) {
                    char c = line.charAt(0);
                    wordMap.put(c, line);
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

    public static class TextAnalysisResult {
        private final int words;
        private final int recognizedCharacters;
        private final int unrecognizedCharacters;

        public TextAnalysisResult(int words, int recognizedCharacters, int unrecognizedCharacters) {
            this.words = words;
            this.recognizedCharacters = recognizedCharacters;
            this.unrecognizedCharacters = unrecognizedCharacters;
        }

        public int getWords() {
            return words;
        }

        public int getRecognizedCharacters() {
            return recognizedCharacters;
        }

        public int getUnrecognizedCharacters() {
            return unrecognizedCharacters;
        }
    }
}
