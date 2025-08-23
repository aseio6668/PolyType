package com.davajava.migrator.core.ast;

public class ClassDeclarationNode extends ASTNode {
    private final String name;
    private final boolean isPublic;

    public ClassDeclarationNode(String name, boolean isPublic, int lineNumber, int columnNumber) {
        super(NodeType.CLASS_DECLARATION, lineNumber, columnNumber);
        this.name = name;
        this.isPublic = isPublic;
    }

    public String getName() { return name; }
    public boolean isPublic() { return isPublic; }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitClassDeclaration(this);
    }
}