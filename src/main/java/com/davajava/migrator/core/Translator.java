package com.davajava.migrator.core;

import com.davajava.migrator.core.ast.ASTNode;

public interface Translator {
    String translate(ASTNode ast) throws TranslationException;
    SourceLanguage getSourceLanguage();
    TranslationOptions getDefaultOptions();
    String translate(ASTNode ast, TranslationOptions options) throws TranslationException;
}