package com.davajava.migrator.translator;

import com.davajava.migrator.core.TranslationException;
import com.davajava.migrator.core.TranslationOptions;
import com.davajava.migrator.core.Translator;
import com.davajava.migrator.core.ast.ASTNode;
import com.davajava.migrator.core.ast.ProgramNode;

public abstract class BaseJavaTranslator implements Translator {
    
    @Override
    public String translate(ASTNode ast) throws TranslationException {
        return translate(ast, getDefaultOptions());
    }

    @Override
    public String translate(ASTNode ast, TranslationOptions options) throws TranslationException {
        if (!(ast instanceof ProgramNode)) {
            throw new TranslationException("Root node must be a ProgramNode");
        }
        
        return generateStubJavaClass();
    }

    @Override
    public TranslationOptions getDefaultOptions() {
        return TranslationOptions.defaultOptions();
    }
    
    protected String generateStubJavaClass() {
        return String.format(
            "// Generated from %s source code\n" +
            "// TODO: Implement %s to Java translation\n" +
            "public class Generated%sClass {\n" +
            "    // TODO: Add translated content here\n" +
            "}\n",
            getSourceLanguage().getDisplayName(),
            getSourceLanguage().getDisplayName(),
            getSourceLanguage().getDisplayName().replace("#", "Sharp")
        );
    }
}