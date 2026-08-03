package me.f1nal.trinity.execution.membersearch;

import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.gui.navigation.NavigationTarget;
import me.f1nal.trinity.util.SearchTermMatchable;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** A stable, display-ready result captured from one member-search pass. */
public final class MemberSearchResult implements SearchTermMatchable {
    private final MemberSearchQuery.Target target;
    private final NavigationTarget navigationTarget;
    private final String name;
    private final String owner;
    private final String kind;
    private final String type;
    private final String descriptor;
    private final String access;
    private final String container;
    private final String packageName;
    private final int referenceCount;
    private final int instructionCount;
    private final String searchableText;
    private final String searchableTextLower;

    MemberSearchResult(MemberSearchQuery.Target target, Input<?> input, String name,
                       String owner, String kind, String type, String descriptor,
                       String access, String container, String packageName,
                       int referenceCount, int instructionCount) {
        this.target = target;
        this.navigationTarget = NavigationTarget.capture(input, null);
        this.name = name;
        this.owner = owner;
        this.kind = kind;
        this.type = type;
        this.descriptor = descriptor;
        this.access = access;
        this.container = container;
        this.packageName = packageName;
        this.referenceCount = referenceCount;
        this.instructionCount = instructionCount;
        this.searchableText = String.join("\n", name, owner, kind, type, descriptor,
                access, container, packageName);
        this.searchableTextLower = searchableText.toLowerCase(Locale.ROOT);
    }

    public MemberSearchQuery.Target target() {
        return target;
    }

    public NavigationTarget navigationTarget() {
        return navigationTarget;
    }

    public @Nullable Input<?> resolve(Trinity trinity) {
        NavigationTarget.ResolvedNavigation resolved = navigationTarget.resolve(trinity);
        return resolved == null ? null : resolved.input();
    }

    public String name() {
        return name;
    }

    public String owner() {
        return owner;
    }

    public String kind() {
        return kind;
    }

    public String type() {
        return type;
    }

    public String descriptor() {
        return descriptor;
    }

    public String access() {
        return access;
    }

    public String container() {
        return container;
    }

    public String packageName() {
        return packageName;
    }

    public int referenceCount() {
        return referenceCount;
    }

    public int instructionCount() {
        return instructionCount;
    }

    @Override
    public boolean matches(String searchTerm) {
        return searchableText.contains(searchTerm);
    }

    @Override
    public boolean matchesIgnoreCase(String searchTerm) {
        return searchableTextLower.contains(searchTerm.toLowerCase(Locale.ROOT));
    }
}
