package com.davajava.migrator.translator.c;

import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.translator.BaseJavaTranslator;

public class CToJavaTranslator extends BaseJavaTranslator {
    @Override
    public SourceLanguage getSourceLanguage() {
        return SourceLanguage.C;
    }
}