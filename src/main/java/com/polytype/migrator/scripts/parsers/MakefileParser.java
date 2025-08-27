package com.polytype.migrator.scripts.parsers;

import com.polytype.migrator.scripts.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Makefile scripts.
 * Extracts targets, variables, dependencies, and build commands.
 */
public class MakefileParser implements ScriptParser {
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(
        "^([A-Z_][A-Z0-9_]*)\\s*[:=]\\s*(.+)$"
    );
    
    private static final Pattern TARGET_PATTERN = Pattern.compile(
        "^([^:]+):\\s*(.*)$"
    );
    
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
        "\\b(gcc|g\\+\\+|clang|clang\\+\\+)\\s+"
    );
    
    private static final Pattern INCLUDE_PATTERN = Pattern.compile(
        "include\\s+([^\\s]+)"
    );
    
    @Override
    public ScriptAnalysisResult parseScript(Path scriptFile) throws IOException {
        ScriptAnalysisResult result = new ScriptAnalysisResult();
        result.setScriptType(ScriptAnalyzer.ScriptType.MAKEFILE);
        result.setFilePath(scriptFile.toString());
        result.setLanguage("Makefile");
        result.setPurpose(ScriptAnalysisResult.ScriptPurpose.BUILD);
        
        List<String> lines = Files.readAllLines(scriptFile);
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Skip comments and empty lines
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }
            
            // Parse variables
            Matcher variableMatcher = VARIABLE_PATTERN.matcher(trimmedLine);
            if (variableMatcher.find()) {
                String varName = variableMatcher.group(1);
                String varValue = variableMatcher.group(2);
                result.addVariable(varName, varValue);
                
                // Detect compiler and language
                if ("CC".equals(varName) || "CXX".equals(varName)) {
                    result.addMetadata("compiler", varValue);
                    if (varValue.contains("g++") || varValue.contains("clang++")) {
                        result.setLanguage("C++");
                        result.addEmbeddedLanguage("C++");
                    } else if (varValue.contains("gcc") || varValue.contains("clang")) {
                        result.setLanguage("C");
                        result.addEmbeddedLanguage("C");
                    }
                }
                continue;
            }
            
            // Parse targets
            Matcher targetMatcher = TARGET_PATTERN.matcher(trimmedLine);
            if (targetMatcher.find()) {
                String targetName = targetMatcher.group(1).trim();
                String dependencies = targetMatcher.group(2).trim();
                
                result.addBuildTarget(targetName);
                
                if (!dependencies.isEmpty()) {
                    String[] deps = dependencies.split("\\s+");
                    for (String dep : deps) {
                        result.addDependency(dep);
                    }
                }
                continue;
            }
            
            // Parse include statements
            Matcher includeMatcher = INCLUDE_PATTERN.matcher(trimmedLine);
            if (includeMatcher.find()) {
                String includeFile = includeMatcher.group(1);
                result.addDependency(includeFile);
                continue;
            }
            
            // Parse command lines (lines starting with tab)
            if (line.startsWith("\t")) {
                String command = line.substring(1).trim();
                result.addCommand(command);
                
                // Detect embedded languages in commands
                if (command.contains("python")) {
                    result.addEmbeddedLanguage("Python");
                } else if (command.contains("node") || command.contains("npm")) {
                    result.addEmbeddedLanguage("JavaScript");
                } else if (command.contains("java") || command.contains("javac")) {
                    result.addEmbeddedLanguage("Java");
                }
            }
        }
        
        // Set default language if not detected
        if (result.getLanguage().equals("Makefile")) {
            if (!result.getEmbeddedLanguages().isEmpty()) {
                result.setLanguage(result.getEmbeddedLanguages().get(0));
            }
        }
        
        // Add common Makefile configuration
        result.addConfiguration("build_system", "make");
        result.addConfiguration("parallel_build", result.getVariables().containsKey("MAKEFLAGS"));
        
        return result;
    }
    
    @Override
    public ScriptAnalyzer.ScriptType getSupportedType() {
        return ScriptAnalyzer.ScriptType.MAKEFILE;
    }
    
    @Override
    public boolean canHandle(Path scriptFile) {
        String filename = scriptFile.getFileName().toString();
        return "Makefile".equals(filename) || "makefile".equals(filename) || 
               filename.endsWith(".mk");
    }
}