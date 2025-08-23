package com.davajava.migrator.core.ast;

public class ProgramNode extends ASTNode {
    public ProgramNode(int lineNumber, int columnNumber) {
        super(NodeType.PROGRAM, lineNumber, columnNumber);
    }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitProgram(this);
    }
}