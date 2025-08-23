package com.davajava.migrator.core.ast;

import java.util.List;

public class FunctionDeclarationNode extends ASTNode {
    private final String name;
    private final String returnType;
    private final List<ParameterNode> parameters;
    private final boolean isPublic;
    private final boolean isStatic;
    private String rawBody; // Store the raw function body

    public FunctionDeclarationNode(String name, String returnType, List<ParameterNode> parameters,
                                 boolean isPublic, boolean isStatic, int lineNumber, int columnNumber) {
        super(NodeType.FUNCTION_DECLARATION, lineNumber, columnNumber);
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.isPublic = isPublic;
        this.isStatic = isStatic;
        
        parameters.forEach(this::addChild);
    }

    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<ParameterNode> getParameters() {
        return parameters;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public String getRawBody() {
        return rawBody;
    }

    public void setRawBody(String rawBody) {
        this.rawBody = rawBody;
    }

    @Override
    public String accept(ASTVisitor visitor) {
        return visitor.visitFunctionDeclaration(this);
    }
}