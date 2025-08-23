package com.davajava.migrator.translator.cpp;

import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.core.TranslationException;
import com.davajava.migrator.core.TranslationOptions;
import com.davajava.migrator.core.Translator;
import com.davajava.migrator.core.ast.ASTNode;

public class CppToJavaTranslator implements Translator {
    private final CppToJavaVisitor visitor;

    public CppToJavaTranslator() {
        this.visitor = new CppToJavaVisitor();
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
            throw new TranslationException("Failed to translate C++ AST to Java", e);
        }
    }

    @Override
    public SourceLanguage getSourceLanguage() {
        return SourceLanguage.CPP;
    }

    @Override
    public TranslationOptions getDefaultOptions() {
        TranslationOptions options = TranslationOptions.defaultOptions();
        options.setOption("cppSpecific.generateImports", true);
        options.setOption("cppSpecific.handleSTL", true);
        options.setOption("cppSpecific.convertPointers", true);
        options.setOption("cppSpecific.addMemoryManagement", false);
        return options;
    }
}