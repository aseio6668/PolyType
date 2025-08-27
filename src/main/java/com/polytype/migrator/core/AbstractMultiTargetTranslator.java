package com.polytype.migrator.core;

import com.polytype.migrator.core.ast.ASTNode;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Abstract base class for multi-target translators.
 * Provides common functionality for translating to multiple target languages.
 */
public abstract class AbstractMultiTargetTranslator implements MultiTargetTranslator {
    protected static final Logger logger = Logger.getLogger(AbstractMultiTargetTranslator.class.getName());
    
    protected final Map<TargetLanguage, TargetVisitor> visitors;
    protected final SourceLanguage sourceLanguage;
    
    public AbstractMultiTargetTranslator(SourceLanguage sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
        this.visitors = new HashMap<>();
        initializeVisitors();
    }
    
    /**
     * Initialize target language visitors for this translator
     */
    protected abstract void initializeVisitors();
    
    @Override
    public String translate(ASTNode ast) throws TranslationException {
        // Default to Java translation for backward compatibility
        return translateTo(ast, TargetLanguage.JAVA);
    }
    
    @Override
    public String translate(ASTNode ast, TranslationOptions options) throws TranslationException {
        // Default to Java translation for backward compatibility
        return translateTo(ast, TargetLanguage.JAVA, options);
    }
    
    @Override
    public String translateTo(ASTNode ast, TargetLanguage targetLanguage) throws TranslationException {
        return translateTo(ast, targetLanguage, getDefaultOptions());
    }
    
    @Override
    public String translateTo(ASTNode ast, TargetLanguage targetLanguage, TranslationOptions options) throws TranslationException {
        if (!supportsTarget(targetLanguage)) {
            throw new TranslationException("Target language " + targetLanguage + " not supported by " + getSourceLanguage() + " translator");
        }
        
        TargetVisitor visitor = visitors.get(targetLanguage);
        if (visitor == null) {
            throw new TranslationException("No visitor available for target language: " + targetLanguage);
        }
        
        try {
            visitor.setOptions(options);
            return ast.accept(visitor);
        } catch (Exception e) {
            throw new TranslationException("Failed to translate " + getSourceLanguage() + " to " + targetLanguage, e);
        }
    }
    
    @Override
    public boolean supportsTarget(TargetLanguage targetLanguage) {
        return visitors.containsKey(targetLanguage);
    }
    
    @Override
    public TargetLanguage[] getSupportedTargets() {
        return visitors.keySet().toArray(new TargetLanguage[0]);
    }
    
    @Override
    public SourceLanguage getSourceLanguage() {
        return sourceLanguage;
    }
    
    /**
     * Register a visitor for a specific target language
     */
    protected void registerVisitor(TargetLanguage targetLanguage, TargetVisitor visitor) {
        visitors.put(targetLanguage, visitor);
    }
    
    /**
     * Get default translation options for this translator
     */
    public TranslationOptions getDefaultOptions() {
        return TranslationOptions.defaultOptions();
    }
}