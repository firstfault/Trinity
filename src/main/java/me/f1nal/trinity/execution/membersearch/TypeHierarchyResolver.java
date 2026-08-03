package me.f1nal.trinity.execution.membersearch;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Resolves project and dependency inheritance without adding dependency classes to search results. */
final class TypeHierarchyResolver {
    enum Result { MATCH, NO_MATCH, UNRESOLVED }

    private final Execution execution;
    private final Map<Key, Result> cache = new HashMap<>();
    private int unresolvedComparisons;

    TypeHierarchyResolver(Execution execution) {
        this.execution = execution;
    }

    Result isSubtype(String child, String parent, boolean direct) {
        if (child.equals(parent)) return Result.MATCH;
        if (parent.equals("java/lang/Object")) return Result.MATCH;
        Key key = new Key(child, parent, direct);
        Result cached = cache.get(key);
        if (cached != null) return cached;

        Result result = direct ? isDirectSubtype(child, parent) : isTransitiveSubtype(child, parent);
        cache.put(key, result);
        if (result == Result.UNRESOLVED) unresolvedComparisons++;
        return result;
    }

    int getUnresolvedComparisons() {
        return unresolvedComparisons;
    }

    private Result isDirectSubtype(String child, String parent) {
        ClassNode node = findNode(child);
        if (node == null) return Result.UNRESOLVED;
        if (parent.equals(node.superName)) return Result.MATCH;
        return node.interfaces != null && node.interfaces.contains(parent)
                ? Result.MATCH : Result.NO_MATCH;
    }

    private Result isTransitiveSubtype(String child, String parent) {
        ArrayDeque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(child);
        boolean unresolved = false;

        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!visited.add(current)) continue;
            if (current.equals(parent)) return Result.MATCH;

            ClassNode node = findNode(current);
            if (node == null) {
                unresolved = true;
                continue;
            }
            if (node.superName != null) queue.addLast(node.superName);
            if (node.interfaces != null) queue.addAll(node.interfaces);
        }
        return unresolved ? Result.UNRESOLVED : Result.NO_MATCH;
    }

    private ClassNode findNode(String internalName) {
        ClassInput projectClass = execution.getClassInput(internalName);
        if (projectClass != null) return projectClass.getNode();
        return execution.getDependencies().getClass(internalName);
    }

    private record Key(String child, String parent, boolean direct) {
    }
}
