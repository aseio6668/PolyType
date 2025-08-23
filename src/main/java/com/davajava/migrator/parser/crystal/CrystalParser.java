package com.davajava.migrator.parser.crystal;

import com.davajava.migrator.core.ParseException;
import com.davajava.migrator.core.Parser;
import com.davajava.migrator.core.SourceLanguage;
import com.davajava.migrator.core.ast.ASTNode;
import com.davajava.migrator.core.ast.ProgramNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CrystalParser implements Parser {
    @Override
    public ASTNode parse(String sourceCode) throws ParseException {
        // TODO: Implement Crystal parsing logic
        ProgramNode program = new ProgramNode(1, 1);
        return program;
    }

    @Override
    public ASTNode parseFile(String filePath) throws IOException, ParseException {
        String content = Files.readString(Paths.get(filePath));
        return parse(content);
    }

    @Override
    public SourceLanguage getSupportedLanguage() {
        return SourceLanguage.CRYSTAL;
    }

    @Override
    public boolean canHandle(String fileName) {
        return fileName.endsWith(".cr");
    }
}