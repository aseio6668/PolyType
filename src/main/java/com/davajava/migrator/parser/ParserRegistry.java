package com.davajava.migrator.parser;

import com.davajava.migrator.core.Parser;
import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.parser.rust.RustParser;
// import com.davajava.migrator.parser.crystal.CrystalParser;
import com.davajava.migrator.parser.c.CParser;
import com.davajava.migrator.parser.cpp.CppParser;
import com.davajava.migrator.parser.python.PythonParser;
import com.davajava.migrator.parser.csharp.CSharpParser;

import java.util.HashMap;
import java.util.Map;

public class ParserRegistry {
    private final Map<SourceLanguage, Parser> parsers;

    public ParserRegistry() {
        this.parsers = new HashMap<>();
        registerParsers();
    }

    private void registerParsers() {
        parsers.put(SourceLanguage.RUST, new RustParser());
        // parsers.put(SourceLanguage.CRYSTAL, new CrystalParser());
        parsers.put(SourceLanguage.C, new CParser());
        parsers.put(SourceLanguage.CPP, new CppParser());
        parsers.put(SourceLanguage.PYTHON, new PythonParser());
        parsers.put(SourceLanguage.CSHARP, new CSharpParser());
    }

    public Parser getParser(SourceLanguage language) {
        return parsers.get(language);
    }

    public void registerParser(SourceLanguage language, Parser parser) {
        parsers.put(language, parser);
    }

    public boolean isSupported(SourceLanguage language) {
        return parsers.containsKey(language);
    }
}