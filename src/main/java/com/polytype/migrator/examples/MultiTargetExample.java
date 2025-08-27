package com.polytype.migrator.examples;

import com.polytype.migrator.core.TargetLanguage;
import com.polytype.migrator.core.TranslationOptions;
import com.polytype.migrator.core.ast.*;
import com.polytype.migrator.translator.kotlin.KotlinMultiTargetTranslator;

import java.util.Arrays;
import java.util.List;

/**
 * Example demonstrating multi-target translation capabilities of PolyType.
 * Shows how to translate Kotlin source code to multiple target languages.
 */
public class MultiTargetExample {
    
    public static void main(String[] args) {
        // Create a sample Kotlin AST
        ProgramNode program = createSampleKotlinAST();
        
        // Create multi-target translator
        KotlinMultiTargetTranslator translator = new KotlinMultiTargetTranslator();
        
        // Demonstrate translation to multiple targets
        System.out.println("=== PolyType Multi-Target Translation Example ===\n");
        
        try {
            // Translate to Java (default/backward compatibility)
            if (translator.supportsTarget(TargetLanguage.JAVA)) {
                System.out.println("🔵 JAVA OUTPUT:");
                System.out.println("─".repeat(50));
                String javaCode = translator.translateTo(program, TargetLanguage.JAVA);
                System.out.println(javaCode);
                System.out.println();
            }
            
            // Translate to C++
            if (translator.supportsTarget(TargetLanguage.CPP)) {
                System.out.println("🔴 C++ OUTPUT:");
                System.out.println("─".repeat(50));
                String cppCode = translator.translateTo(program, TargetLanguage.CPP);
                System.out.println(cppCode);
                System.out.println();
            }
            
            // Translate to Python
            if (translator.supportsTarget(TargetLanguage.PYTHON)) {
                System.out.println("🐍 PYTHON OUTPUT:");
                System.out.println("─".repeat(50));
                String pythonCode = translator.translateTo(program, TargetLanguage.PYTHON);
                System.out.println(pythonCode);
                System.out.println();
            }
            
            // Translate to TypeScript
            if (translator.supportsTarget(TargetLanguage.TYPESCRIPT)) {
                System.out.println("📘 TYPESCRIPT OUTPUT:");
                System.out.println("─".repeat(50));
                String tsCode = translator.translateTo(program, TargetLanguage.TYPESCRIPT);
                System.out.println(tsCode);
                System.out.println();
            }
            
            // Translate to Go
            if (translator.supportsTarget(TargetLanguage.GO)) {
                System.out.println("🔵 GO OUTPUT:");
                System.out.println("─".repeat(50));
                String goCode = translator.translateTo(program, TargetLanguage.GO);
                System.out.println(goCode);
                System.out.println();
            }
            
            // Translate to C#
            if (translator.supportsTarget(TargetLanguage.CSHARP)) {
                System.out.println("💜 C# OUTPUT:");
                System.out.println("─".repeat(50));
                String csharpCode = translator.translateTo(program, TargetLanguage.CSHARP);
                System.out.println(csharpCode);
                System.out.println();
            }
            
            // Show supported targets
            System.out.println("🎯 SUPPORTED TARGET LANGUAGES:");
            System.out.println("─".repeat(50));
            for (TargetLanguage target : translator.getSupportedTargets()) {
                System.out.println("✓ " + target.getDisplayName() + " (" + target.getExtension() + ")");
            }
            System.out.println();
            
            // Demonstrate cross-language translation matrix
            System.out.println("🌐 CROSS-LANGUAGE TRANSLATION MATRIX:");
            System.out.println("─".repeat(50));
            System.out.println("Source: Kotlin → Targets: " + translator.getSupportedTargets().size() + " languages");
            System.out.println("• Modern languages: Java, C#, TypeScript, Go");
            System.out.println("• Systems languages: C++, Rust (planned)");
            System.out.println("• Dynamic languages: Python, JavaScript (planned)");
            System.out.println("• Mobile languages: Swift (planned), Kotlin (native)");
            System.out.println();
            
            // Demonstrate with custom options for different targets
            System.out.println("🔧 CUSTOM OPTIONS EXAMPLES:");
            System.out.println("─".repeat(50));
            
            // C++ with modern features
            TranslationOptions cppOptions = TranslationOptions.defaultOptions();
            cppOptions.setOption("cpp.standard", "20");
            cppOptions.setOption("cpp.useNamespaces", true);
            cppOptions.setOption("cpp.useSmartPointers", true);
            
            if (translator.supportsTarget(TargetLanguage.CPP)) {
                System.out.println("C++ with C++20 features:");
                String customCppCode = translator.translateTo(program, TargetLanguage.CPP, cppOptions);
                System.out.println(customCppCode.substring(0, Math.min(200, customCppCode.length())) + "...");
                System.out.println();
            }
            
            // Python with type hints
            TranslationOptions pythonOptions = TranslationOptions.defaultOptions();
            pythonOptions.setOption("python.useTypeHints", true);
            pythonOptions.setOption("python.useDataclasses", true);
            pythonOptions.setOption("python.pythonVersion", "3.9");
            
            if (translator.supportsTarget(TargetLanguage.PYTHON)) {
                System.out.println("Python with type hints and dataclasses:");
                String customPythonCode = translator.translateTo(program, TargetLanguage.PYTHON, pythonOptions);
                System.out.println(customPythonCode.substring(0, Math.min(200, customPythonCode.length())) + "...");
                System.out.println();
            }
            
            // TypeScript with strict mode
            TranslationOptions tsOptions = TranslationOptions.defaultOptions();
            tsOptions.setOption("typescript.strict", true);
            tsOptions.setOption("typescript.useInterfaces", true);
            tsOptions.setOption("typescript.generateFactories", true);
            
            if (translator.supportsTarget(TargetLanguage.TYPESCRIPT)) {
                System.out.println("TypeScript with strict mode:");
                String customTsCode = translator.translateTo(program, TargetLanguage.TYPESCRIPT, tsOptions);
                System.out.println(customTsCode.substring(0, Math.min(200, customTsCode.length())) + "...");
                System.out.println();
            }
            
        } catch (Exception e) {
            System.err.println("Translation error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates a sample Kotlin AST representing:
     * 
     * data class User(val name: String, val age: Int) {
     *     fun greet(): String {
     *         return "Hello, I'm $name"
     *     }
     * }
     */
    private static ProgramNode createSampleKotlinAST() {
        ProgramNode program = new ProgramNode(1, 1);
        
        // Create User data class
        ClassDeclarationNode userClass = new ClassDeclarationNode("User", true, 1, 1);
        
        // Add properties
        VariableDeclarationNode nameProp = new VariableDeclarationNode(
            "name", "String", false, null, 2, 5);
        VariableDeclarationNode ageProp = new VariableDeclarationNode(
            "age", "Int", false, null, 2, 20);
        
        userClass.addChild(nameProp);
        userClass.addChild(ageProp);
        
        // Add constructor
        List<ParameterNode> constructorParams = Arrays.asList(
            new ParameterNode("name", "String", false, 1, 15),
            new ParameterNode("age", "Int", false, 1, 30)
        );
        
        FunctionDeclarationNode constructor = new FunctionDeclarationNode(
            "User", "void", constructorParams, true, false, 1, 1);
        userClass.addChild(constructor);
        
        // Add greet method
        FunctionDeclarationNode greetMethod = new FunctionDeclarationNode(
            "greet", "String", Arrays.asList(), true, false, 4, 5);
        userClass.addChild(greetMethod);
        
        program.addChild(userClass);
        return program;
    }
}