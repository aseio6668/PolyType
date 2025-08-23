package com.davajava.migrator.core.ast;

public interface ASTVisitor {
    String visitProgram(ProgramNode node);
    String visitFunctionDeclaration(FunctionDeclarationNode node);
    String visitVariableDeclaration(VariableDeclarationNode node);
    String visitClassDeclaration(ClassDeclarationNode node);
    String visitStructDeclaration(StructDeclarationNode node);
    String visitExpression(ExpressionNode node);
    String visitBinaryExpression(BinaryExpressionNode node);
    String visitUnaryExpression(UnaryExpressionNode node);
    String visitLiteral(LiteralNode node);
    String visitIdentifier(IdentifierNode node);
    String visitBlockStatement(BlockStatementNode node);
    String visitIfStatement(IfStatementNode node);
    String visitWhileLoop(WhileLoopNode node);
    String visitForLoop(ForLoopNode node);
    String visitReturnStatement(ReturnStatementNode node);
    String visitAssignment(AssignmentNode node);
    String visitFunctionCall(FunctionCallNode node);
    String visitMethodCall(MethodCallNode node);
    String visitFieldAccess(FieldAccessNode node);
    String visitArrayAccess(ArrayAccessNode node);
    String visitTypeAnnotation(TypeAnnotationNode node);
    String visitParameter(ParameterNode node);
    String visitComment(CommentNode node);
}