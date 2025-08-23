package com.davajava.migrator.core.ast;

public class FieldDeclarationNode extends ASTNode {
    private final String name;
    private final String type;
    private final boolean isPublic;
    private final boolean isMutable;

    public FieldDeclarationNode(String name, String type, boolean isPublic, 
                              boolean isMutable, int lineNumber, int columnNumber) {
        super(NodeType.VARIABLE_DECLARATION, lineNumber, columnNumber);
        this.name = name;
        this.type = type;
        this.isPublic = isPublic;
        this.isMutable = isMutable;
    }

    public String getName() {
        return name;
    }

    public String getDataType() {
        return type;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean isMutable() {
        return isMutable;
    }

    @Override
    public String accept(ASTVisitor visitor) {
        // Field declarations are treated as variable declarations for visiting
        VariableDeclarationNode varNode = new VariableDeclarationNode(
            this.name, this.type, this.isMutable, null, 
            this.getLineNumber(), this.getColumnNumber()
        );
        return visitor.visitVariableDeclaration(varNode);
    }
}