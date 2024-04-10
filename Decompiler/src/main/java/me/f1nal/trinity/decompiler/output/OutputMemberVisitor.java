package me.f1nal.trinity.decompiler.output;

import me.f1nal.trinity.decompiler.output.impl.*;

public interface OutputMemberVisitor {
    void visitBytecodeMarker(BytecodeMarkerOutputMember bytecodeMarker);
    void visitClass(ClassOutputMember member);
    void visitComment(CommentOutputMember comment);
    void visitNumber(NumberOutputMember constant);
    void visitFieldDeclaration(FieldDeclarationOutputMember fieldDeclaration);
    void visitField(FieldOutputMember field);
    void visitKeyword(KeywordOutputMember keyword);
    void visitMethod(MethodOutputMember method);
    void visitMethodStartEnd(MethodStartEndOutputMember methodStartEnd);
    void visitPackage(PackageOutputMember pkg);
    void visitString(StringOutputMember string);
    void visitVariable(VariableOutputMember variable);
    void visitKind(KindOutputMember kind);
}
