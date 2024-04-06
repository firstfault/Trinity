package me.f1nal.trinity.execution.xref;

public enum XrefAccessType {
    READ("R"),
    WRITE("W"),
    EXECUTE("X"),
    ;

    private final String text;

    XrefAccessType(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
