package com.davajava.migrator.translator.scala;

import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.core.TranslationException;
import com.davajava.migrator.core.TranslationOptions;
import com.davajava.migrator.core.Translator;
import com.davajava.migrator.core.ast.ASTNode;

public class ScalaToJavaTranslator implements Translator {
    private final ScalaToJavaVisitor visitor;

    public ScalaToJavaTranslator() {
        this.visitor = new ScalaToJavaVisitor();
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
            throw new TranslationException("Failed to translate Scala AST to Java", e);
        }
    }

    @Override
    public SourceLanguage getSourceLanguage() {
        return SourceLanguage.SCALA;
    }

    @Override
    public TranslationOptions getDefaultOptions() {
        TranslationOptions options = TranslationOptions.defaultOptions();
        options.setOption("scalaSpecific.generateImports", true);
        options.setOption("scalaSpecific.convertCaseClasses", true);
        options.setOption("scalaSpecific.handleTraits", true);
        options.setOption("scalaSpecific.convertObjects", true);
        options.setOption("scalaSpecific.handleFunctional", true);
        return options;
    }
}