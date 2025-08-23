package com.davajava.migrator.core;

import com.davajava.migrator.core.ast.ASTNode;
import java.io.IOException;

public interface Parser {
    ASTNode parse(String sourceCode) throws ParseException;
    ASTNode parseFile(String filePath) throws IOException, ParseException;
    SourceLanguage getSupportedLanguage();
    boolean canHandle(String fileName);
}