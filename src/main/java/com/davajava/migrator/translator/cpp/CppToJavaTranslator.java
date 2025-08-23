package com.davajava.migrator.translator.cpp;

import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.translator.BaseJavaTranslator;

public class CppToJavaTranslator extends BaseJavaTranslator {
    @Override
    public SourceLanguage getSourceLanguage() {
        return SourceLanguage.CPP;
    }
}