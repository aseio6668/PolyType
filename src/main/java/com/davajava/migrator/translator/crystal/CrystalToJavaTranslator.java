package com.davajava.migrator.translator.crystal;

import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.translator.BaseJavaTranslator;

public class CrystalToJavaTranslator extends BaseJavaTranslator {
    @Override
    public SourceLanguage getSourceLanguage() {
        return SourceLanguage.CRYSTAL;
    }
}