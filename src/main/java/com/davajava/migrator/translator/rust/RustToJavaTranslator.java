package com.davajava.migrator.translator.rust;

import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.core.TranslationException;
import com.davajava.migrator.core.TranslationOptions;
import com.davajava.migrator.core.Translator;
import com.davajava.migrator.core.ast.ASTNode;

public class RustToJavaTranslator implements Translator {
    private final RustToJavaVisitor visitor;

    public RustToJavaTranslator() {
        this.visitor = new RustToJavaVisitor();
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
            throw new TranslationException("Failed to translate Rust AST to Java", e);
        }
    }

    @Override
    public SourceLanguage getSourceLanguage() {
        return SourceLanguage.RUST;
    }

    @Override
    public TranslationOptions getDefaultOptions() {
        TranslationOptions options = TranslationOptions.defaultOptions();
        options.setOption("rustSpecific.handleOwnership", true);
        options.setOption("rustSpecific.convertTraits", true);
        options.setOption("rustSpecific.handleLifetimes", false);
        return options;
    }
}