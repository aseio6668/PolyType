package com.polytype.migrator.translator.kotlin;

import com.polytype.migrator.core.AbstractMultiTargetTranslator;
import com.polytype.migrator.core.SourceLanguage;
import com.polytype.migrator.core.TargetLanguage;
import com.polytype.migrator.translator.cpp.CppTargetVisitor;
import com.polytype.migrator.translator.python.PythonTargetVisitor;
import com.polytype.migrator.translator.typescript.TypeScriptTargetVisitor;
import com.polytype.migrator.translator.go.GoTargetVisitor;
import com.polytype.migrator.translator.csharp.CSharpTargetVisitor;

/**
 * Multi-target translator for Kotlin source code.
 * Supports translation to Java, C++, Python, TypeScript, Go, C#, and other target languages.
 */
public class KotlinMultiTargetTranslator extends AbstractMultiTargetTranslator {
    
    public KotlinMultiTargetTranslator() {
        super(SourceLanguage.KOTLIN);
    }
    
    @Override
    protected void initializeVisitors() {
        // Register Java visitor (backward compatibility)
        registerVisitor(TargetLanguage.JAVA, new KotlinToJavaVisitor());
        
        // Register C++ visitor
        registerVisitor(TargetLanguage.CPP, new CppTargetVisitor());
        
        // Register Python visitor
        registerVisitor(TargetLanguage.PYTHON, new PythonTargetVisitor());
        
        // Register TypeScript visitor
        registerVisitor(TargetLanguage.TYPESCRIPT, new TypeScriptTargetVisitor());
        
        // Register Go visitor
        registerVisitor(TargetLanguage.GO, new GoTargetVisitor());
        
        // Register C# visitor
        registerVisitor(TargetLanguage.CSHARP, new CSharpTargetVisitor());
        
        // Additional target languages can be added here
        // registerVisitor(TargetLanguage.JAVASCRIPT, new JavaScriptTargetVisitor());
        // registerVisitor(TargetLanguage.RUST, new RustTargetVisitor());
        // registerVisitor(TargetLanguage.SWIFT, new SwiftTargetVisitor());
    }
    
    /**
     * Kotlin-specific Java visitor that extends the base visitor
     */
    private static class KotlinToJavaVisitor extends com.polytype.migrator.translator.kotlin.KotlinToJavaVisitor 
            implements com.polytype.migrator.core.TargetVisitor {
        
        private com.polytype.migrator.core.TranslationOptions options;
        
        @Override
        public void setOptions(com.polytype.migrator.core.TranslationOptions options) {
            this.options = options;
            // Apply options to parent visitor
            super.setOptions(options);
        }
        
        @Override
        public TargetLanguage getTargetLanguage() {
            return TargetLanguage.JAVA;
        }
        
        @Override
        public com.polytype.migrator.core.TranslationOptions getDefaultOptions() {
            com.polytype.migrator.core.TranslationOptions defaultOptions = 
                com.polytype.migrator.core.TranslationOptions.defaultOptions();
            defaultOptions.setOption("kotlin.generateNullChecks", true);
            defaultOptions.setOption("kotlin.convertCoroutines", true);
            defaultOptions.setOption("kotlin.useOptional", true);
            return defaultOptions;
        }
        
        @Override
        public String generateImports() {
            return "import java.util.*;\n" +
                   "import java.util.concurrent.CompletableFuture;\n" +
                   "import java.util.function.*;\n";
        }
        
        @Override
        public String generateFileHeader() {
            return "// Generated from Kotlin source code\n" +
                   "// Migrated using PolyType Code Migrator\n\n" +
                   generateImports() + "\n";
        }
        
        @Override
        public String generateFileFooter() {
            return "\n// End of generated Java code\n";
        }
    }
}