package me.f1nal.trinity.execution.membersearch;

import me.f1nal.trinity.execution.packages.Package;
import me.f1nal.trinity.execution.packages.ProjectContainer;
import me.f1nal.trinity.util.INameable;

import java.util.Map;

/** Immutable criteria captured from the Member Search form. */
public record MemberSearchQuery(Target target, Scope scope, Common common,
                                ClassCriteria classCriteria,
                                FieldCriteria fieldCriteria,
                                MethodCriteria methodCriteria) {
    public MemberSearchQuery {
        scope = scope == null ? Scope.project() : scope;
        common = common == null ? Common.defaults() : common;
        classCriteria = classCriteria == null ? ClassCriteria.defaults() : classCriteria;
        fieldCriteria = fieldCriteria == null ? FieldCriteria.defaults() : fieldCriteria;
        methodCriteria = methodCriteria == null ? MethodCriteria.defaults() : methodCriteria;
    }

    public static MemberSearchQuery defaults(Target target) {
        return new MemberSearchQuery(target, Scope.project(), Common.defaults(),
                ClassCriteria.defaults(), FieldCriteria.defaults(), MethodCriteria.defaults());
    }

    public enum Target implements INameable {
        CLASS("Class"), FIELD("Field"), METHOD("Method");

        private final String name;

        Target(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum ScopeKind implements INameable {
        PROJECT("Whole Project"), INPUT("Imported Archive"), PACKAGE("Package");

        private final String name;

        ScopeKind(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum TextMode implements INameable {
        CONTAINS("Contains"), EXACT("Exact"), REGEX("Regex");

        private final String name;

        TextMode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum DescriptorMode implements INameable {
        EXACT("Exact"), REGEX("Regex");

        private final String name;

        DescriptorMode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum Visibility implements INameable {
        ANY("Any Visibility"), PUBLIC("Public"), PROTECTED("Protected"),
        PACKAGE_PRIVATE("Package-private"), PRIVATE("Private");

        private final String name;

        Visibility(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum FlagMode implements INameable {
        IGNORE("Ignore"), REQUIRE("Required"), EXCLUDE("Excluded");

        private final String name;

        FlagMode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        public FlagMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public enum ClassKind implements INameable {
        ANY("Any Kind"), CLASS("Class"), INTERFACE("Interface"), ENUM("Enum"),
        ANNOTATION("Annotation"), RECORD("Record");

        private final String name;

        ClassKind(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum TypeMode implements INameable {
        EXACT("Exact"), ASSIGNABLE_TO("Assignable To"), ASSIGNABLE_FROM("Assignable From");

        private final String name;

        TypeMode(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum HierarchyDepth implements INameable {
        DIRECT("Direct"), TRANSITIVE("Transitive");

        private final String name;

        HierarchyDepth(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum RenameState implements INameable {
        ANY("Any Name State"), ORIGINAL("Original"), RENAMED("Renamed");

        private final String name;

        RenameState(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum ReferenceState implements INameable {
        ANY("Any Reference State"), REFERENCED("Referenced"), UNREFERENCED("Unreferenced");

        private final String name;

        ReferenceState(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum MethodKind implements INameable {
        ANY("Any Method"), REGULAR("Regular Method"), CONSTRUCTOR("Constructor"),
        STATIC_INITIALIZER("Static Initializer");

        private final String name;

        MethodKind(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum BodyState implements INameable {
        ANY("Any Body"), HAS_BODY("Has Executable Code"), NO_BODY("No Executable Code");

        private final String name;

        BodyState(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public enum AnnotationLocation implements INameable {
        DECLARATION_OR_PARAMETER("Declaration or Parameter"),
        DECLARATION("Declaration"), PARAMETER("Parameter");

        private final String name;

        AnnotationLocation(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public record Scope(ScopeKind kind, ProjectContainer container, Package pkg,
                        boolean includeSubpackages) {
        public static Scope project() {
            return new Scope(ScopeKind.PROJECT, null, null, true);
        }
    }

    public record TextCriterion(String text, TextMode mode, boolean caseSensitive) {
        public TextCriterion {
            text = text == null ? "" : text.trim();
            mode = mode == null ? TextMode.CONTAINS : mode;
        }

        public boolean active() {
            return !text.isEmpty();
        }
    }

    public record TypeCriterion(String text, TypeMode mode) {
        public TypeCriterion {
            text = text == null ? "" : text.trim();
            mode = mode == null ? TypeMode.EXACT : mode;
        }

        public static TypeCriterion empty() {
            return new TypeCriterion("", TypeMode.EXACT);
        }

        public boolean active() {
            return !text.isEmpty();
        }
    }

    public record IntRange(int minimum, int maximum) {
        public static IntRange any() {
            return new IntRange(-1, -1);
        }

        public boolean active() {
            return minimum >= 0 || maximum >= 0;
        }

        public boolean contains(int value) {
            return (minimum < 0 || value >= minimum) && (maximum < 0 || value <= maximum);
        }
    }

    public record Common(TextCriterion name, Visibility visibility, Map<Integer, FlagMode> flags,
                         ClassKind ownerKind, TypeCriterion declaringClass,
                         String descriptor, DescriptorMode descriptorMode,
                         String genericType, String annotationType,
                         AnnotationLocation annotationLocation, RenameState renameState,
                         ReferenceState referenceState, IntRange referenceRange) {
        public Common {
            name = name == null ? new TextCriterion("", TextMode.CONTAINS, false) : name;
            visibility = visibility == null ? Visibility.ANY : visibility;
            flags = flags == null ? Map.of() : Map.copyOf(flags);
            ownerKind = ownerKind == null ? ClassKind.ANY : ownerKind;
            declaringClass = declaringClass == null ? TypeCriterion.empty() : declaringClass;
            descriptor = descriptor == null ? "" : descriptor.trim();
            descriptorMode = descriptorMode == null ? DescriptorMode.EXACT : descriptorMode;
            genericType = genericType == null ? "" : genericType.trim();
            annotationType = annotationType == null ? "" : annotationType.trim();
            annotationLocation = annotationLocation == null
                    ? AnnotationLocation.DECLARATION_OR_PARAMETER : annotationLocation;
            renameState = renameState == null ? RenameState.ANY : renameState;
            referenceState = referenceState == null ? ReferenceState.ANY : referenceState;
            referenceRange = referenceRange == null ? IntRange.any() : referenceRange;
        }

        public static Common defaults() {
            return new Common(new TextCriterion("", TextMode.CONTAINS, false), Visibility.ANY,
                    Map.of(), ClassKind.ANY, TypeCriterion.empty(), "", DescriptorMode.EXACT,
                    "", "", AnnotationLocation.DECLARATION_OR_PARAMETER,
                    RenameState.ANY, ReferenceState.ANY, IntRange.any());
        }
    }

    public record ClassCriteria(ClassKind kind, TypeCriterion baseType, HierarchyDepth depth) {
        public ClassCriteria {
            kind = kind == null ? ClassKind.ANY : kind;
            baseType = baseType == null ? TypeCriterion.empty() : baseType;
            depth = depth == null ? HierarchyDepth.TRANSITIVE : depth;
        }

        public static ClassCriteria defaults() {
            return new ClassCriteria(ClassKind.ANY, TypeCriterion.empty(), HierarchyDepth.TRANSITIVE);
        }
    }

    public record FieldCriteria(TypeCriterion declaredType) {
        public FieldCriteria {
            declaredType = declaredType == null ? TypeCriterion.empty() : declaredType;
        }

        public static FieldCriteria defaults() {
            return new FieldCriteria(TypeCriterion.empty());
        }
    }

    public record MethodCriteria(MethodKind kind, TypeCriterion returnType,
                                 TypeCriterion parameterType, String exactParameters,
                                 IntRange parameterCount, BodyState bodyState,
                                 IntRange instructionCount) {
        public MethodCriteria {
            kind = kind == null ? MethodKind.ANY : kind;
            returnType = returnType == null ? TypeCriterion.empty() : returnType;
            parameterType = parameterType == null ? TypeCriterion.empty() : parameterType;
            exactParameters = exactParameters == null ? "" : exactParameters.trim();
            parameterCount = parameterCount == null ? IntRange.any() : parameterCount;
            bodyState = bodyState == null ? BodyState.ANY : bodyState;
            instructionCount = instructionCount == null ? IntRange.any() : instructionCount;
        }

        public static MethodCriteria defaults() {
            return new MethodCriteria(MethodKind.ANY, TypeCriterion.empty(), TypeCriterion.empty(),
                    "", IntRange.any(), BodyState.ANY, IntRange.any());
        }
    }
}
