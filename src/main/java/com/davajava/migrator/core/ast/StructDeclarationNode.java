package com.davajava.migrator.core.ast;

import java.util.ArrayList;
import java.util.List;

public class StructDeclarationNode extends ASTNode {
    private final String name;
    private final boolean isPublic;
    private final List<FieldDeclarationNode> fields;

    public StructDeclarationNode(String name, boolean isPublic, 
                               List<FieldDeclarationNode> fields, 
                               int lineNumber, int columnNumber) {
        super(NodeType.STRUCT_DECLARATION, lineNumber, columnNumber);
        this.name = name;
        this.isPublic = isPublic;
        this.fields = new ArrayList<>(fields);
        
        fields.forEach(this::addChild);
    }

    public String getName() {
        return name;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public List<FieldDeclarationNode> getFields() {
        return new ArrayList<>(fields);
    }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitStructDeclaration(this);
    }
}