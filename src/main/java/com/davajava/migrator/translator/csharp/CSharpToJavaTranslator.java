package com.davajava.migrator.translator.csharp;

import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.translator.BaseJavaTranslator;

public class CSharpToJavaTranslator extends BaseJavaTranslator {
    @Override
    public SourceLanguage getSourceLanguage() {
        return SourceLanguage.CSHARP;
    }
}