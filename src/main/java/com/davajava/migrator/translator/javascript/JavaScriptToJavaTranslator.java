package com.davajava.migrator.translator.javascript;

import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.core.TranslationException;
import com.davajava.migrator.core.TranslationOptions;
import com.davajava.migrator.core.Translator;
import com.davajava.migrator.core.ast.ASTNode;

public class JavaScriptToJavaTranslator implements Translator {
    private final JavaScriptToJavaVisitor visitor;

    public JavaScriptToJavaTranslator() {
        this.visitor = new JavaScriptToJavaVisitor();
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
            throw new TranslationException("Failed to translate JavaScript/TypeScript AST to Java", e);
        }
    }

    @Override
    public SourceLanguage getSourceLanguage() {
        return SourceLanguage.JAVASCRIPT;
    }

    @Override
    public TranslationOptions getDefaultOptions() {
        TranslationOptions options = TranslationOptions.defaultOptions();
        options.setOption("jsSpecific.generateImports", true);
        options.setOption("jsSpecific.handlePromises", true);
        options.setOption("jsSpecific.convertArrowFunctions", true);
        options.setOption("jsSpecific.handleTypescript", true);
        options.setOption("jsSpecific.generateUtilMethods", true);
        options.setOption("jsSpecific.strictTypeInference", false);
        return options;
    }
}