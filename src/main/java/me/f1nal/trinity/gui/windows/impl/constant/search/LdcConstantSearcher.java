package me.f1nal.trinity.gui.windows.impl.constant.search;

import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.execution.FieldInput;
import me.f1nal.trinity.execution.MethodInput;
import me.f1nal.trinity.execution.constant.AsmConstantScanner;
import me.f1nal.trinity.execution.xref.XrefKind;
import me.f1nal.trinity.execution.xref.where.XrefWhere;
import me.f1nal.trinity.execution.xref.where.XrefWhereClass;
import me.f1nal.trinity.execution.xref.where.XrefWhereField;
import me.f1nal.trinity.execution.xref.where.XrefWhereMethod;
import me.f1nal.trinity.execution.xref.where.XrefWhereMethodInsn;
import me.f1nal.trinity.gui.windows.impl.constant.ConstantViewCache;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Searches actual constants throughout loaded ASM trees.
 *
 * <p>The historical name is retained for compatibility. The underlying scanner
 * covers fields, annotations, LDC, bootstrap data, switches and numeric bytecode
 * constants from one canonical traversal shared with occurrence statistics.</p>
 */
public abstract class LdcConstantSearcher<T> {
    public void populate(List<ConstantViewCache> list, Execution execution) {
        for (ClassInput classInput : new ArrayList<>(execution.getClassList())) {
            populateClass(list, classInput);
        }
    }

    void populateClass(List<ConstantViewCache> list, ClassInput classInput) {
        Map<FieldNode, FieldInput> fields = new IdentityHashMap<>();
        classInput.getFieldMap().values().forEach(field -> fields.put(field.getNode(), field));
        Map<MethodNode, MethodInput> methods = new IdentityHashMap<>();
        classInput.getMethodMap().values().forEach(method -> methods.put(method.getNode(), method));
        XrefWhereClass classWhere = new XrefWhereClass(classInput);

        for (AsmConstantScanner.Occurrence occurrence :
                AsmConstantScanner.scan(classInput.getNode())) {
            if (!isOfType(occurrence.value())) continue;
            //noinspection unchecked
            String constant = convertConstantToText((T) occurrence.value());
            if (constant == null) continue;
            list.add(new ConstantViewCache(constant,
                    createWhere(occurrence, classWhere, fields, methods),
                    occurrence.kind() == AsmConstantScanner.Kind.ANNOTATION
                            ? XrefKind.ANNOTATION : XrefKind.LITERAL));
        }
    }

    private XrefWhere createWhere(AsmConstantScanner.Occurrence occurrence,
                                  XrefWhereClass classWhere,
                                  Map<FieldNode, FieldInput> fields,
                                  Map<MethodNode, MethodInput> methods) {
        AsmConstantScanner.Site site = occurrence.site();
        if (site.method() != null) {
            MethodInput method = methods.get(site.method());
            if (method != null) {
                if (site.instruction() != null
                        && occurrence.kind() == AsmConstantScanner.Kind.LITERAL) {
                    return new XrefWhereMethodInsn(method, site.instruction(),
                            occurrence.value(), occurrence.instructionOccurrence());
                }
                return new XrefWhereMethod(method);
            }
        }
        if (site.field() != null) {
            FieldInput field = fields.get(site.field());
            if (field != null) return new XrefWhereField(field);
        }
        return classWhere;
    }

    protected abstract boolean isOfType(Object value);

    protected abstract String convertConstantToText(T value);
}
