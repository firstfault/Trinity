package me.f1nal.trinity.gui.windows.impl.constant.search;

import imgui.ImGui;
import imgui.flag.ImGuiDataType;
import imgui.type.ImDouble;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImLong;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.gui.windows.impl.assembler.args.NumberArgument;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewCache;

import java.util.List;

public abstract class ConstantSearchTypeNumber extends ConstantSearchType {
    public ConstantSearchTypeNumber(Trinity trinity, String name) {
        super(name, trinity);
    }

    @Override
    public void populate(List<ConstantViewCache> list) {
        new LdcConstantSearcher<Number>() {
            @Override
            protected boolean isOfType(Object value) {
                return value instanceof Number;
            }

            @Override
            protected String convertConstantToText(Number value) {
                return ConstantSearchTypeNumber.this.convertConstantToText(value);
            }
        }.populate(list, getTrinity().getExecution());
    }

    protected abstract String convertConstantToText(Number value);

    public static class ConstantSearchTypeFloat extends ConstantSearchTypeNumber {
        private final ImFloat value = new ImFloat();

        public ConstantSearchTypeFloat(Trinity trinity) {
            super(trinity, "Float");
        }

        public ConstantSearchTypeFloat(Trinity trinity, float value) {
            this(trinity);
            this.value.set(value);
        }

        @Override
        public boolean draw() {
            ImGui.inputScalar("Float", ImGuiDataType.Float, value, 1.F, 5.F);
            return true;
        }

        @Override
        protected String convertConstantToText(Number value) {
            if (value instanceof Float) {
                if (value.floatValue() != this.value.get()) {
                    return null;
                }
                return value.floatValue() + "F";
            }
            return null;
        }
    }

    public static class ConstantSearchTypeDouble extends ConstantSearchTypeNumber {
        private final ImDouble value = new ImDouble();

        public ConstantSearchTypeDouble(Trinity trinity) {
            super(trinity, "Double");
        }

        public ConstantSearchTypeDouble(Trinity trinity, double value) {
            this(trinity);
            this.value.set(value);
        }

        @Override
        public boolean draw() {
            ImGui.inputScalar("Double", ImGuiDataType.Double, value, 1.D, 5.D);
            return true;
        }

        @Override
        protected String convertConstantToText(Number value) {
            if (value instanceof Double) {
                if (value.doubleValue() != this.value.get()) {
                    return null;
                }
                return value.doubleValue() + "D";
            }
            return null;
        }
    }

    public static class ConstantSearchTypeInteger extends ConstantSearchTypeNumber {
        private final ImInt value = new ImInt();

        public ConstantSearchTypeInteger(Trinity trinity) {
            super(trinity, "Integer");
        }

        public ConstantSearchTypeInteger(Trinity trinity, int value) {
            this(trinity);
            this.value.set(value);
        }

        @Override
        public boolean draw() {
            ImGui.inputScalar("Integer", ImGuiDataType.S32, value, 1, 5);
            return true;
        }

        @Override
        protected String convertConstantToText(Number value) {
            if (value instanceof Integer || value instanceof Byte || value instanceof Short) {
                if (value.intValue() != this.value.get()) {
                    return null;
                }
                return String.valueOf(value.intValue());
            }
            return null;
        }
    }

    public static class ConstantSearchTypeLong extends ConstantSearchTypeNumber {
        private final ImLong value = new ImLong();

        public ConstantSearchTypeLong(Trinity trinity) {
            super(trinity, "Long");
        }

        public ConstantSearchTypeLong(Trinity trinity, long value) {
            this(trinity);
            this.value.set(value);
        }

        @Override
        public boolean draw() {
            ImGui.inputScalar("Long Integer", ImGuiDataType.S64, value, 1, 5);
            return true;
        }

        @Override
        protected String convertConstantToText(Number value) {
            if (value instanceof Long) {
                if (value.intValue() != this.value.get()) {
                    return null;
                }
                return value.intValue() + "L";
            }
            return null;
        }
    }

    public static class ConstantSearchTypeDecimal extends ConstantSearchTypeNumber {
        private final ImLong value = new ImLong();

        public ConstantSearchTypeDecimal(Trinity trinity) {
            super(trinity, "Decimal (Any Type)");
        }

        @Override
        public boolean draw() {
            ImGui.inputScalar("Decimal", ImGuiDataType.S64, value, 1, 5);
            return true;
        }

        @Override
        protected String convertConstantToText(Number value) {
            if (value.intValue() != this.value.get()) {
                return null;
            }
            return value.intValue() + NumberArgument.getPrefix(value);
        }
    }
}
