package com.polytype.migrator.core;

import com.polytype.migrator.core.ast.ASTNode;

/**
 * Interface for translating AST nodes to multiple target languages.
 * Extends the base Translator interface to support multi-target output.
 */
public interface MultiTargetTranslator extends Translator {
    
    /**
     * Translate AST to the specified target language
     */
    String translateTo(ASTNode ast, TargetLanguage targetLanguage) throws TranslationException;
    
    /**
     * Translate AST to the specified target language with options
     */
    String translateTo(ASTNode ast, TargetLanguage targetLanguage, TranslationOptions options) throws TranslationException;
    
    /**
     * Check if this translator supports the given target language
     */
    boolean supportsTarget(TargetLanguage targetLanguage);
    
    /**
     * Get all supported target languages for this translator
     */
    TargetLanguage[] getSupportedTargets();
    
    /**
     * Get the primary source language this translator handles
     */
    SourceLanguage getSourceLanguage();
}