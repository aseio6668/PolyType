package com.davajava.migrator.core.ast;

public class VariableDeclarationNode extends ASTNode {
    private final String name;
    private final String type;
    private final boolean isMutable;
    private final ASTNode initializer;

    public VariableDeclarationNode(String name, String type, boolean isMutable, 
                                 ASTNode initializer, int lineNumber, int columnNumber) {
        super(NodeType.VARIABLE_DECLARATION, lineNumber, columnNumber);
        this.name = name;
        this.type = type;
        this.isMutable = isMutable;
        this.initializer = initializer;
        if (initializer != null) {
            addChild(initializer);
        }
    }

    public String getName() { return name; }
    public String getDataType() { return type; }
    public boolean isMutable() { return isMutable; }
    public ASTNode getInitializer() { return initializer; }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitVariableDeclaration(this);
    }
}