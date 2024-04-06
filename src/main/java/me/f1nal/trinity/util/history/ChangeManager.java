package me.f1nal.trinity.util.history;

import java.util.Stack;

public class ChangeManager<T extends Changeable> {
    private final Stack<T> stack = new Stack<>();

    public T getLast() {
        return stack.peek();
    }

    public T removeLast() {
        return stack.pop();
    }

    public void add(T change) {
        stack.push(change);
    }

    public Stack<T> getStack() {
        return stack;
    }

    public void undo(T history) {
        history.undo();
        stack.remove(history);
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

//    public void redo(T history) {
//        history.redo();
//        history
//    }
}
