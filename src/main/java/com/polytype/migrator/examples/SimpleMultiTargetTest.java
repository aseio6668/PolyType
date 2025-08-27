package com.polytype.migrator.examples;

import com.polytype.migrator.core.TargetLanguage;
import com.polytype.migrator.core.ast.*;
import com.polytype.migrator.translator.kotlin.KotlinMultiTargetTranslator;
import java.util.Arrays;

/**
 * Simple test to verify multi-target translation functionality
 */
public class SimpleMultiTargetTest {
    
    public static void main(String[] args) {
        System.out.println("=== PolyType Multi-Target Translation Test ===\n");
        
        // Create a simple Kotlin AST for testing
        ProgramNode program = createSimpleProgram();
        
        // Create multi-target translator
        KotlinMultiTargetTranslator translator = new KotlinMultiTargetTranslator();
        
        // Test each supported target language
        System.out.println("Testing translation to supported targets:\n");
        
        for (TargetLanguage target : translator.getSupportedTargets()) {
            try {
                System.out.println("✓ Testing " + target.getDisplayName() + " (" + target.getExtension() + "):");
                String code = translator.translateTo(program, target);
                System.out.println("  Generated " + code.split("\n").length + " lines of code");
                System.out.println("  First 100 characters: " + 
                    code.substring(0, Math.min(100, code.length())).replace("\n", "\\n"));
                System.out.println();
            } catch (Exception e) {
                System.err.println("✗ Failed to translate to " + target.getDisplayName() + ": " + e.getMessage());
                System.out.println();
            }
        }
        
        System.out.println("Multi-target translation test completed!");
        System.out.println("Total supported targets: " + translator.getSupportedTargets().size());
    }
    
    /**
     * Creates a simple program for testing: a basic class with a field and method
     */
    private static ProgramNode createSimpleProgram() {
        ProgramNode program = new ProgramNode(1, 1);
        
        // Create a simple class: class TestClass { String name; String getName() {...} }
        ClassDeclarationNode testClass = new ClassDeclarationNode("TestClass", false, 1, 1);
        
        // Add a field
        VariableDeclarationNode nameField = new VariableDeclarationNode(
            "name", "String", false, "\"test\"", 2, 5);
        testClass.addChild(nameField);
        
        // Add a method
        FunctionDeclarationNode getNameMethod = new FunctionDeclarationNode(
            "getName", "String", Arrays.asList(), true, false, 4, 5);
        testClass.addChild(getNameMethod);
        
        program.addChild(testClass);
        return program;
    }
}