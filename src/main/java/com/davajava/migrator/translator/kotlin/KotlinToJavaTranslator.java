package com.davajava.migrator.translator.kotlin;

import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.core.TranslationException;
import com.davajava.migrator.core.TranslationOptions;
import com.davajava.migrator.core.Translator;
import com.davajava.migrator.core.ast.ASTNode;

public class KotlinToJavaTranslator implements Translator {
    private final KotlinToJavaVisitor visitor;

    public KotlinToJavaTranslator() {
        this.visitor = new KotlinToJavaVisitor();
    }

    @Override
    public String translate(ASTNode ast) throws TranslationException {
        return translate(ast, getDefaultOptions());
    }

    @Override
    public String translate(ASTNode ast, TranslationOptions options) throws TranslationException {
        try {
            visitor.setOptions(options);
            return ast.accept(visitor);
        } catch (Exception e) {
            throw new TranslationException("Failed to translate Kotlin AST to Java", e);
        }
    }

    @Override
    public SourceLanguage getSourceLanguage() {
        return SourceLanguage.KOTLIN;
    }

    @Override
    public TranslationOptions getDefaultOptions() {
        TranslationOptions options = TranslationOptions.defaultOptions();
        options.setOption("kotlinSpecific.generateImports", true);
        options.setOption("kotlinSpecific.convertExtensions", true);
        options.setOption("kotlinSpecific.handleNullability", true);
        options.setOption("kotlinSpecific.convertDataClasses", true);
        options.setOption("kotlinSpecific.convertCoroutines", false); // Advanced feature
        return options;
    }
}