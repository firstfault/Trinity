package me.f1nal.trinity.refactor.identity;

import java.util.Objects;

/** One concrete classfile value changed by an identity refactor. */
public record IdentityRefactorChange(
        Category category,
        String className,
        String location,
        String before,
        String after) {

    public IdentityRefactorChange {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
    }

    public enum Category {
        DECLARATION("Declaration"),
        BYTECODE("Bytecode"),
        DESCRIPTOR("Descriptor"),
        SIGNATURE("Signature"),
        ANNOTATION("Annotation"),
        METADATA("Metadata"),
        CONSTANT("Constant"),
        MODULE("Module");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
