package com.davajava.migrator.translator.python;

import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.core.TranslationException;
import com.davajava.migrator.core.TranslationOptions;
import com.davajava.migrator.core.Translator;
import com.davajava.migrator.core.ast.ASTNode;

public class PythonToJavaTranslator implements Translator {
    private final PythonToJavaVisitor visitor;

    public PythonToJavaTranslator() {
        this.visitor = new PythonToJavaVisitor();
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
            throw new TranslationException("Failed to translate Python AST to Java", e);
        }
    }

    @Override
    public SourceLanguage getSourceLanguage() {
        return SourceLanguage.PYTHON;
    }

    @Override
    public TranslationOptions getDefaultOptions() {
        TranslationOptions options = TranslationOptions.defaultOptions();
        options.setOption("pythonSpecific.generateImports", true);
        options.setOption("pythonSpecific.handleDuckTyping", true);
        options.setOption("pythonSpecific.addTypeComments", true);
        return options;
    }
}