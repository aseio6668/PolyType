package com.davajava.migrator.translator.go;

import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.core.TranslationException;
import com.davajava.migrator.core.TranslationOptions;
import com.davajava.migrator.core.Translator;
import com.davajava.migrator.core.ast.ASTNode;

public class GoToJavaTranslator implements Translator {
    private final GoToJavaVisitor visitor;

    public GoToJavaTranslator() {
        this.visitor = new GoToJavaVisitor();
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
            throw new TranslationException("Failed to translate Go AST to Java", e);
        }
    }

    @Override
    public SourceLanguage getSourceLanguage() {
        return SourceLanguage.GO;
    }

    @Override
    public TranslationOptions getDefaultOptions() {
        TranslationOptions options = TranslationOptions.defaultOptions();
        options.setOption("goSpecific.generateImports", true);
        options.setOption("goSpecific.handleChannels", true);
        options.setOption("goSpecific.convertGoroutines", true);
        options.setOption("goSpecific.handleInterfaces", true);
        options.setOption("goSpecific.convertSlices", true);
        options.setOption("goSpecific.handleErrors", true);
        options.setOption("goSpecific.generateUtilMethods", true);
        return options;
    }
}