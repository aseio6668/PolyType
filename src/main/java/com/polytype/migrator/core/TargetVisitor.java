package com.polytype.migrator.core;

import com.polytype.migrator.core.ast.ASTVisitor;

/**
 * Interface for visitors that generate code in specific target languages.
 * Extends ASTVisitor to provide target language-specific code generation.
 */
public interface TargetVisitor extends ASTVisitor {
    
    /**
     * Set translation options for this visitor
     */
    void setOptions(TranslationOptions options);
    
    /**
     * Get the target language this visitor generates
     */
    TargetLanguage getTargetLanguage();
    
    /**
     * Get default options for this target language
     */
    TranslationOptions getDefaultOptions();
    
    /**
     * Generate target language-specific imports/includes
     */
    String generateImports();
    
    /**
     * Generate target language-specific file header
     */
    String generateFileHeader();
    
    /**
     * Generate target language-specific file footer
     */
    String generateFileFooter();
}