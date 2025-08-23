package com.davajava.migrator.parser.rust;

import com.davajava.migrator.core.ParseException;
import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.core.ast.ASTNode;
import com.davajava.migrator.core.ast.FunctionDeclarationNode;
import com.davajava.migrator.core.ast.ProgramNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RustParserTest {
    private RustParser parser;

    @BeforeEach
    void setUp() {
        parser = new RustParser();
    }

    @Test
    void testGetSupportedLanguage() {
        assertEquals(SourceLanguage.RUST, parser.getSupportedLanguage());
    }

    @Test
    void testCanHandle() {
        assertTrue(parser.canHandle("test.rs"));
        assertFalse(parser.canHandle("test.java"));
        assertFalse(parser.canHandle("test.py"));
    }

    @Test
    void testParseSimpleFunction() throws ParseException {
        String rustCode = "pub fn hello() -> i32 {\n    42\n}";
        
        ASTNode result = parser.parse(rustCode);
        
        assertNotNull(result);
        assertTrue(result instanceof ProgramNode);
        
        ProgramNode program = (ProgramNode) result;
        assertEquals(1, program.getChildren().size());
        
        ASTNode child = program.getChildren().get(0);
        assertTrue(child instanceof FunctionDeclarationNode);
        
        FunctionDeclarationNode function = (FunctionDeclarationNode) child;
        assertEquals("hello", function.getName());
        assertEquals("int", function.getReturnType());
        assertTrue(function.isPublic());
        assertEquals(0, function.getParameters().size());
    }

    @Test
    void testParseFunctionWithParameters() throws ParseException {
        String rustCode = "fn add(a: i32, b: i32) -> i32 {\n    a + b\n}";
        
        ASTNode result = parser.parse(rustCode);
        ProgramNode program = (ProgramNode) result;
        FunctionDeclarationNode function = (FunctionDeclarationNode) program.getChildren().get(0);
        
        assertEquals("add", function.getName());
        assertEquals("int", function.getReturnType());
        assertFalse(function.isPublic());
        assertEquals(2, function.getParameters().size());
        
        assertEquals("a", function.getParameters().get(0).getName());
        assertEquals("int", function.getParameters().get(0).getType());
        assertEquals("b", function.getParameters().get(1).getName());
        assertEquals("int", function.getParameters().get(1).getType());
    }

    @Test
    void testParseVoidFunction() throws ParseException {
        String rustCode = "pub fn print_hello() {\n    println!(\"Hello\");\n}";
        
        ASTNode result = parser.parse(rustCode);
        ProgramNode program = (ProgramNode) result;
        FunctionDeclarationNode function = (FunctionDeclarationNode) program.getChildren().get(0);
        
        assertEquals("print_hello", function.getName());
        assertEquals("void", function.getReturnType());
        assertTrue(function.isPublic());
    }

    @Test
    void testParseMultipleFunctions() throws ParseException {
        String rustCode = 
            "pub fn first() -> i32 { 1 }\n" +
            "fn second(x: String) -> bool { true }";
        
        ASTNode result = parser.parse(rustCode);
        ProgramNode program = (ProgramNode) result;
        
        assertEquals(2, program.getChildren().size());
        
        FunctionDeclarationNode first = (FunctionDeclarationNode) program.getChildren().get(0);
        assertEquals("first", first.getName());
        assertTrue(first.isPublic());
        
        FunctionDeclarationNode second = (FunctionDeclarationNode) program.getChildren().get(1);
        assertEquals("second", second.getName());
        assertFalse(second.isPublic());
        assertEquals(1, second.getParameters().size());
        assertEquals("String", second.getParameters().get(0).getType());
    }
}