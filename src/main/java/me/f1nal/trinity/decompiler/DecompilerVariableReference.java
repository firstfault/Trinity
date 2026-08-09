package me.f1nal.trinity.decompiler;

import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerLineText;
import me.f1nal.trinity.gui.windows.impl.entryviewer.impl.decompiler.DecompilerComponent;

import java.util.List;

public record DecompilerVariableReference(MethodInput methodInput, int variableIndex,
                                          int componentOccurrence,
                                          DecompilerVariableAccess access,
                                          int lineNumber, String lineText,
                                          List<DecompilerLineText> lineComponents,
                                          DecompilerComponent accessComponent) {
}
