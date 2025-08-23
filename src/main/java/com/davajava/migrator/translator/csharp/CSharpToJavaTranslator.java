package com.davajava.migrator.translator.csharp;

import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.core.TranslationException;
import com.davajava.migrator.core.TranslationOptions;
import com.davajava.migrator.core.Translator;
import com.davajava.migrator.core.ast.ASTNode;

public class CSharpToJavaTranslator implements Translator {
    private final CSharpToJavaVisitor visitor;

    public CSharpToJavaTranslator() {
        this.visitor = new CSharpToJavaVisitor();
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
            throw new TranslationException("Failed to translate C# AST to Java", e);
        }
    }

    @Override
    public SourceLanguage getSourceLanguage() {
        return SourceLanguage.CSHARP;
    }

    @Override
    public TranslationOptions getDefaultOptions() {
        TranslationOptions options = TranslationOptions.defaultOptions();
        options.setOption("csharpSpecific.generateImports", true);
        options.setOption("csharpSpecific.convertProperties", true);
        options.setOption("csharpSpecific.handleNullable", true);
        options.setOption("csharpSpecific.convertLinq", true);
        return options;
    }
}