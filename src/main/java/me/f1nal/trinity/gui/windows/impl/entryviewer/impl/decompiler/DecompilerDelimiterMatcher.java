package me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Resolves structural delimiter pairs while ignoring Java literals and comments. */
final class DecompilerDelimiterMatcher {
    private DecompilerDelimiterMatcher() {
    }

    static Match findMatch(List<DecompilerLine> lines, DecompilerCoordinates selected) {
        if (selected == null || selected.getLine() == null) return null;
        String selectedLine = selected.getLine().getText();
        int selectedCharacter = selected.getCharacter();
        if (selectedCharacter < 0 || selectedCharacter >= selectedLine.length()
                || !selected.getLine().isRawDecompilerTextAtCharacter(selectedCharacter)
                || !isDelimiter(selectedLine.charAt(selectedCharacter))) {
            return null;
        }

        Deque<OpenDelimiter> delimiters = new ArrayDeque<>();
        LexicalState state = LexicalState.CODE;
        for (DecompilerLine line : lines) {
            String text = line.getText();
            for (int index = 0; index < text.length(); index++) {
                if (!line.isRawDecompilerTextAtCharacter(index)) continue;
                char character = text.charAt(index);

                if (state == LexicalState.BLOCK_COMMENT) {
                    if (character == '*' && hasNext(line, text, index, '/')) {
                        state = LexicalState.CODE;
                        index++;
                    }
                    continue;
                }
                if (state == LexicalState.TEXT_BLOCK) {
                    if (startsWithTripleQuote(line, text, index) && !isEscaped(line, text, index)) {
                        state = LexicalState.CODE;
                        index += 2;
                    }
                    continue;
                }
                if (state == LexicalState.STRING || state == LexicalState.CHARACTER) {
                    char terminator = state == LexicalState.STRING ? '"' : '\'';
                    if (character == '\\') {
                        index++;
                    } else if (character == terminator) {
                        state = LexicalState.CODE;
                    }
                    continue;
                }

                if (character == '/' && hasNext(line, text, index, '/')) break;
                if (character == '/' && hasNext(line, text, index, '*')) {
                    state = LexicalState.BLOCK_COMMENT;
                    index++;
                    continue;
                }
                if (startsWithTripleQuote(line, text, index)) {
                    state = LexicalState.TEXT_BLOCK;
                    index += 2;
                    continue;
                }
                if (character == '"') {
                    state = LexicalState.STRING;
                    continue;
                }
                if (character == '\'') {
                    state = LexicalState.CHARACTER;
                    continue;
                }

                DecompilerCoordinates coordinates = new DecompilerCoordinates(line, index);
                if (isOpening(character)) {
                    delimiters.push(new OpenDelimiter(character, coordinates));
                    continue;
                }
                if (!isClosing(character) || delimiters.isEmpty()
                        || delimiters.peek().delimiter() != openingFor(character)) {
                    continue;
                }

                OpenDelimiter opening = delimiters.pop();
                if (selected.equals(opening.coordinates())) {
                    return new Match(selected, coordinates);
                }
                if (selected.equals(coordinates)) {
                    return new Match(selected, opening.coordinates());
                }
            }

            // Ordinary Java strings and character literals cannot span source lines. Recovering
            // here prevents malformed decompiler output from hiding every following delimiter.
            if (state == LexicalState.STRING || state == LexicalState.CHARACTER) {
                state = LexicalState.CODE;
            }
        }
        return null;
    }

    static boolean isDelimiter(char character) {
        return isOpening(character) || isClosing(character);
    }

    private static boolean isOpening(char character) {
        return character == '(' || character == '[' || character == '{';
    }

    private static boolean isClosing(char character) {
        return character == ')' || character == ']' || character == '}';
    }

    private static char openingFor(char closing) {
        return switch (closing) {
            case ')' -> '(';
            case ']' -> '[';
            case '}' -> '{';
            default -> '\0';
        };
    }

    private static boolean hasNext(DecompilerLine line, String text, int index, char expected) {
        return index + 1 < text.length()
                && line.isRawDecompilerTextAtCharacter(index + 1)
                && text.charAt(index + 1) == expected;
    }

    private static boolean startsWithTripleQuote(DecompilerLine line, String text, int index) {
        return index + 2 < text.length()
                && line.isRawDecompilerTextAtCharacter(index + 1)
                && line.isRawDecompilerTextAtCharacter(index + 2)
                && text.charAt(index) == '"'
                && text.charAt(index + 1) == '"'
                && text.charAt(index + 2) == '"';
    }

    private static boolean isEscaped(DecompilerLine line, String text, int index) {
        int slashCount = 0;
        for (int cursor = index - 1; cursor >= 0
                && line.isRawDecompilerTextAtCharacter(cursor)
                && text.charAt(cursor) == '\\'; cursor--) {
            slashCount++;
        }
        return (slashCount & 1) != 0;
    }

    record Match(DecompilerCoordinates selected, DecompilerCoordinates matching) {
    }

    private record OpenDelimiter(char delimiter, DecompilerCoordinates coordinates) {
    }

    private enum LexicalState {
        CODE,
        STRING,
        CHARACTER,
        BLOCK_COMMENT,
        TEXT_BLOCK
    }
}
