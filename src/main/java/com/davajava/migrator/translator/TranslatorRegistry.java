package com.davajava.migrator.translator;

import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.core.Translator;
import com.davajava.migrator.translator.rust.RustToJavaTranslator;
// import com.davajava.migrator.translator.crystal.CrystalToJavaTranslator;
import com.davajava.migrator.translator.c.CToJavaTranslator;
import com.davajava.migrator.translator.cpp.CppToJavaTranslator;
import com.davajava.migrator.translator.python.PythonToJavaTranslator;
import com.davajava.migrator.translator.csharp.CSharpToJavaTranslator;

import java.util.HashMap;
import java.util.Map;

public class TranslatorRegistry {
    private final Map<SourceLanguage, Translator> translators;

    public TranslatorRegistry() {
        this.translators = new HashMap<>();
        registerTranslators();
    }

    private void registerTranslators() {
        translators.put(SourceLanguage.RUST, new RustToJavaTranslator());
        // translators.put(SourceLanguage.CRYSTAL, new CrystalToJavaTranslator());
        translators.put(SourceLanguage.C, new CToJavaTranslator());
        translators.put(SourceLanguage.CPP, new CppToJavaTranslator());
        translators.put(SourceLanguage.PYTHON, new PythonToJavaTranslator());
        translators.put(SourceLanguage.CSHARP, new CSharpToJavaTranslator());
    }

    public Translator getTranslator(SourceLanguage language) {
        return translators.get(language);
    }

    public void registerTranslator(SourceLanguage language, Translator translator) {
        translators.put(language, translator);
    }

    public boolean isSupported(SourceLanguage language) {
        return translators.containsKey(language);
    }
}