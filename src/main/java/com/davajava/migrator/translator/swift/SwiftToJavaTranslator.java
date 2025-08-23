package com.davajava.migrator.translator.swift;

import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.core.TranslationException;
import com.davajava.migrator.core.TranslationOptions;
import com.davajava.migrator.core.Translator;
import com.davajava.migrator.core.ast.ASTNode;

public class SwiftToJavaTranslator implements Translator {
    private final SwiftToJavaVisitor visitor;

    public SwiftToJavaTranslator() {
        this.visitor = new SwiftToJavaVisitor();
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
            throw new TranslationException("Failed to translate Swift AST to Java", e);
        }
    }

    @Override
    public SourceLanguage getSourceLanguage() {
        return SourceLanguage.SWIFT;
    }

    @Override
    public TranslationOptions getDefaultOptions() {
        TranslationOptions options = TranslationOptions.defaultOptions();
        options.setOption("swiftSpecific.generateImports", true);
        options.setOption("swiftSpecific.handleOptionals", true);
        options.setOption("swiftSpecific.convertProtocols", true);
        options.setOption("swiftSpecific.handleEnums", true);
        options.setOption("swiftSpecific.convertClosures", true);
        options.setOption("swiftSpecific.handleStructs", true);
        options.setOption("swiftSpecific.generateUtilMethods", true);
        options.setOption("swiftSpecific.convertSwiftUI", false);
        return options;
    }
}