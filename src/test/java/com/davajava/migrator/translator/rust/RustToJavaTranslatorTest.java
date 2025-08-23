package com.davajava.migrator.translator.rust;

import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.core.TranslationException;
import com.davajava.migrator.core.ast.FunctionDeclarationNode;
import com.davajava.migrator.core.ast.ParameterNode;
import com.davajava.migrator.core.ast.ProgramNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RustToJavaTranslatorTest {
    private RustToJavaTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new RustToJavaTranslator();
    }

    @Test
    void testGetSourceLanguage() {
        assertEquals(SourceLanguage.RUST, translator.getSourceLanguage());
    }

    @Test
    void testTranslateSimpleFunction() throws TranslationException {
        ProgramNode program = new ProgramNode(1, 1);
        
        List<ParameterNode> params = new ArrayList<>();
        params.add(new ParameterNode("a", "int", false, 1, 1));
        params.add(new ParameterNode("b", "int", false, 1, 1));
        
        FunctionDeclarationNode function = new FunctionDeclarationNode(
            "add", "int", params, true, false, 1, 1
        );
        
        program.addChild(function);
        
        String javaCode = translator.translate(program);
        
        assertNotNull(javaCode);
        assertTrue(javaCode.contains("public int add(int a, int b)"));
        assertTrue(javaCode.contains("return 0;"));
        assertTrue(javaCode.contains("Generated from Rust"));
    }

    @Test
    void testTranslateVoidFunction() throws TranslationException {
        ProgramNode program = new ProgramNode(1, 1);
        
        FunctionDeclarationNode function = new FunctionDeclarationNode(
            "printHello", "void", new ArrayList<>(), false, false, 1, 1
        );
        
        program.addChild(function);
        
        String javaCode = translator.translate(program);
        
        assertNotNull(javaCode);
        assertTrue(javaCode.contains("private void printHello()"));
        assertFalse(javaCode.contains("return"));
    }

    @Test
    void testDefaultOptions() {
        var options = translator.getDefaultOptions();
        
        assertNotNull(options);
        assertTrue(options.getBooleanOption("generateComments", false));
        assertTrue(options.getBooleanOption("preserveOriginalNames", false));
    }
}