package me.f1nal.trinity.decompiler.output.number;

import me.f1nal.trinity.util.INameable;

public enum NumberDisplayTypeEnum implements INameable {
    DECIMAL("Decimal", new NumberDisplayTypeDecimal()),
    HEX("Hex", new NumberDisplayTypeHex()),
    OCTAL("Octal", new NumberDisplayTypeOctal()),
    BINARY("Binary", new NumberDisplayTypeBinary()),
    ASCII("ASCII", new NumberDisplayTypeChar()),
    ;
    private final String name;
    private final NumberDisplayType instance;

    NumberDisplayTypeEnum(String name, NumberDisplayType instance) {
        this.name = name;
        this.instance = instance;
    }

    @Override
    public String getName() {
        return name;
    }

    public NumberDisplayType getInstance() {
        return instance;
    }
}
