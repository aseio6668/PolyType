package com.polytype.migrator.scripts.parsers;

import com.polytype.migrator.scripts.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for shell scripts (.sh, .bash, .zsh files).
 * Analyzes shell script structure, commands, dependencies, and purpose.
 */
public class ShellScriptParser implements ScriptParser {
    
    @Override
    public ScriptAnalyzer.ScriptType getSupportedType() {
        return ScriptAnalyzer.ScriptType.SHELL_SCRIPT;
    }
    
    @Override
    public boolean canHandle(Path scriptFile) {
        String fileName = scriptFile.getFileName().toString().toLowerCase();
        return fileName.endsWith(".sh") || fileName.endsWith(".bash") || fileName.endsWith(".zsh");
    }
    
    // Pattern for shebang line
    private static final Pattern SHEBANG_PATTERN = Pattern.compile("^#!\\s*(.+)$");
    
    // Pattern for variable assignments
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("^\\s*(\\w+)\\s*=\\s*(.+)$");
    
    // Pattern for function definitions
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("^\\s*(\\w+)\\s*\\(\\s*\\)\\s*\\{?\\s*$");
    
    // Pattern for commands that indicate build processes
    private static final Pattern BUILD_COMMAND_PATTERN = Pattern.compile(
        "\\b(make|cmake|gcc|g\\+\\+|clang|javac|mvn|gradle|npm|yarn|cargo|go|python|pip)\\b"
    );
    
    // Pattern for dependency management commands
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
        "\\b(apt|yum|brew|pip|npm|yarn|cargo|go get)\\s+(?:install\\s+)?([^\\s]+)"
    );
    
    // Pattern for common script purposes
    private static final Pattern BUILD_PURPOSE_PATTERN = Pattern.compile(
        "\\b(build|compile|make|install|deploy|package|release)\\b", Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TEST_PURPOSE_PATTERN = Pattern.compile(
        "\\b(test|spec|check|verify)\\b", Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SETUP_PURPOSE_PATTERN = Pattern.compile(
        "\\b(setup|init|configure|install|bootstrap)\\b", Pattern.CASE_INSENSITIVE
    );

    @Override
    public ScriptAnalysisResult parseScript(Path scriptFile) throws IOException {
        ScriptAnalysisResult result = new ScriptAnalysisResult();
        result.setScriptType(ScriptAnalyzer.ScriptType.SHELL_SCRIPT);
        result.setFilePath(scriptFile.toString());
        result.setLanguage("Shell");
        
        List<String> lines = Files.readAllLines(scriptFile);
        if (lines.isEmpty()) {
            result.setPurpose(ScriptAnalysisResult.ScriptPurpose.UNKNOWN);
            return result;
        }
        
        String fileName = scriptFile.getFileName().toString().toLowerCase();
        analyzeFileName(fileName, result);
        
        // Parse content line by line
        String currentFunction = null;
        boolean inFunction = false;
        int braceCount = 0;
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            
            // Skip empty lines and comments (but analyze comments for purpose)
            if (line.isEmpty()) continue;
            
            if (line.startsWith("#")) {
                if (i == 0 && line.startsWith("#!")) {
                    // Parse shebang
                    parseShebang(line, result);
                } else {
                    // Analyze comment for purpose clues
                    analyzeComment(line, result);
                }
                continue;
            }
            
            // Parse variable assignments
            Matcher variableMatcher = VARIABLE_PATTERN.matcher(line);
            if (variableMatcher.find()) {
                String varName = variableMatcher.group(1);
                String varValue = variableMatcher.group(2).replaceAll("[\"']", "");
                result.addVariable(varName, varValue);
                
                // Check for common configuration variables
                analyzeConfigVariable(varName, varValue, result);
                continue;
            }
            
            // Parse function definitions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(line);
            if (functionMatcher.find()) {
                currentFunction = functionMatcher.group(1);
                inFunction = true;
                braceCount = line.contains("{") ? 1 : 0;
                result.addMetadata("function_" + currentFunction, "defined");
                continue;
            }
            
            // Track brace counting for functions
            if (inFunction) {
                braceCount += countOccurrences(line, '{') - countOccurrences(line, '}');
                if (braceCount <= 0) {
                    inFunction = false;
                    currentFunction = null;
                }
            }
            
            // Analyze commands
            analyzeCommand(line, result, currentFunction);
        }
        
        // Determine overall purpose if not already set
        if (result.getPurpose() == ScriptAnalysisResult.ScriptPurpose.UNKNOWN) {
            result.setPurpose(inferPurposeFromContent(result));
        }
        
        // Add embedded languages based on commands
        detectEmbeddedLanguages(result);
        
        return result;
    }
    
    private void analyzeFileName(String fileName, ScriptAnalysisResult result) {
        if (BUILD_PURPOSE_PATTERN.matcher(fileName).find()) {
            result.setPurpose(ScriptAnalysisResult.ScriptPurpose.BUILD);
        } else if (TEST_PURPOSE_PATTERN.matcher(fileName).find()) {
            result.setPurpose(ScriptAnalysisResult.ScriptPurpose.TEST);
        } else if (SETUP_PURPOSE_PATTERN.matcher(fileName).find()) {
            result.setPurpose(ScriptAnalysisResult.ScriptPurpose.SETUP);
        } else if (fileName.contains("deploy")) {
            result.setPurpose(ScriptAnalysisResult.ScriptPurpose.DEPLOYMENT);
        } else if (fileName.contains("run") || fileName.contains("start")) {
            result.setPurpose(ScriptAnalysisResult.ScriptPurpose.RUNTIME);
        }
    }
    
    private void parseShebang(String shebangLine, ScriptAnalysisResult result) {
        Matcher matcher = SHEBANG_PATTERN.matcher(shebangLine);
        if (matcher.find()) {
            String interpreter = matcher.group(1).trim();
            result.addMetadata("interpreter", interpreter);
            
            // Determine shell type
            if (interpreter.contains("bash")) {
                result.setLanguage("Bash");
                result.addEmbeddedLanguage("Bash");
            } else if (interpreter.contains("zsh")) {
                result.setLanguage("Zsh");
                result.addEmbeddedLanguage("Zsh");
            } else if (interpreter.contains("sh")) {
                result.setLanguage("Shell");
                result.addEmbeddedLanguage("Shell");
            } else if (interpreter.contains("fish")) {
                result.setLanguage("Fish");
                result.addEmbeddedLanguage("Fish");
            }
        }
    }
    
    private void analyzeComment(String comment, ScriptAnalysisResult result) {
        String content = comment.substring(1).trim().toLowerCase();
        
        if (BUILD_PURPOSE_PATTERN.matcher(content).find()) {
            result.setPurpose(ScriptAnalysisResult.ScriptPurpose.BUILD);
        } else if (TEST_PURPOSE_PATTERN.matcher(content).find()) {
            result.setPurpose(ScriptAnalysisResult.ScriptPurpose.TEST);
        } else if (SETUP_PURPOSE_PATTERN.matcher(content).find()) {
            result.setPurpose(ScriptAnalysisResult.ScriptPurpose.SETUP);
        }
        
        // Look for version information
        if (content.contains("version") && content.matches(".*\\d+\\.\\d+.*")) {
            String[] parts = content.split("\\s+");
            for (String part : parts) {
                if (part.matches("\\d+\\.\\d+.*")) {
                    result.setVersion(part);
                    break;
                }
            }
        }
    }
    
    private void analyzeConfigVariable(String varName, String varValue, ScriptAnalysisResult result) {
        String name = varName.toLowerCase();
        
        // Common configuration variables
        if (name.contains("version")) {
            result.setVersion(varValue);
        } else if (name.contains("java_home") || name.contains("jdk")) {
            result.addEmbeddedLanguage("Java");
            result.addDependency("Java JDK");
        } else if (name.contains("python") || name.contains("pip")) {
            result.addEmbeddedLanguage("Python");
            result.addDependency("Python");
        } else if (name.contains("node") || name.contains("npm")) {
            result.addEmbeddedLanguage("JavaScript");
            result.addDependency("Node.js");
        } else if (name.contains("cargo") || name.contains("rust")) {
            result.addEmbeddedLanguage("Rust");
            result.addDependency("Rust");
        } else if (name.contains("go")) {
            result.addEmbeddedLanguage("Go");
            result.addDependency("Go");
        }
        
        // Build targets
        if (name.contains("target") || name.contains("output") || name.contains("dest")) {
            result.addBuildTarget(varValue);
        }
    }
    
    private void analyzeCommand(String line, ScriptAnalysisResult result, String currentFunction) {
        result.addCommand(line);
        
        // Look for build commands
        Matcher buildMatcher = BUILD_COMMAND_PATTERN.matcher(line);
        if (buildMatcher.find()) {
            String command = buildMatcher.group(1);
            
            switch (command) {
                case "make":
                case "cmake":
                    result.addEmbeddedLanguage("C++");
                    result.addDependency("Build Tools");
                    break;
                case "gcc":
                case "g++":
                case "clang":
                    result.addEmbeddedLanguage("C++");
                    result.addDependency("C++ Compiler");
                    break;
                case "javac":
                case "mvn":
                case "gradle":
                    result.addEmbeddedLanguage("Java");
                    result.addDependency("Java JDK");
                    break;
                case "npm":
                case "yarn":
                    result.addEmbeddedLanguage("JavaScript");
                    result.addDependency("Node.js");
                    break;
                case "cargo":
                    result.addEmbeddedLanguage("Rust");
                    result.addDependency("Rust");
                    break;
                case "go":
                    result.addEmbeddedLanguage("Go");
                    result.addDependency("Go");
                    break;
                case "python":
                case "pip":
                    result.addEmbeddedLanguage("Python");
                    result.addDependency("Python");
                    break;
            }
            
            if (result.getPurpose() == ScriptAnalysisResult.ScriptPurpose.UNKNOWN) {
                result.setPurpose(ScriptAnalysisResult.ScriptPurpose.BUILD);
            }
        }
        
        // Look for dependency installation commands
        Matcher depMatcher = DEPENDENCY_PATTERN.matcher(line);
        if (depMatcher.find()) {
            String packageManager = depMatcher.group(1);
            String dependency = depMatcher.group(2);
            
            result.addDependency(dependency);
            
            switch (packageManager) {
                case "pip":
                    result.addEmbeddedLanguage("Python");
                    break;
                case "npm":
                case "yarn":
                    result.addEmbeddedLanguage("JavaScript");
                    break;
                case "cargo":
                    result.addEmbeddedLanguage("Rust");
                    break;
                case "go":
                    result.addEmbeddedLanguage("Go");
                    break;
            }
        }
        
        // Look for test commands
        if (line.contains("test") || line.contains("spec") || line.contains("check")) {
            if (result.getPurpose() == ScriptAnalysisResult.ScriptPurpose.UNKNOWN) {
                result.setPurpose(ScriptAnalysisResult.ScriptPurpose.TEST);
            }
        }
        
        // Look for deployment commands
        if (line.contains("deploy") || line.contains("publish") || line.contains("release")) {
            if (result.getPurpose() == ScriptAnalysisResult.ScriptPurpose.UNKNOWN) {
                result.setPurpose(ScriptAnalysisResult.ScriptPurpose.DEPLOYMENT);
            }
        }
    }
    
    private ScriptAnalysisResult.ScriptPurpose inferPurposeFromContent(ScriptAnalysisResult result) {
        // Check commands for purpose hints
        for (String command : result.getCommands()) {
            if (BUILD_COMMAND_PATTERN.matcher(command).find()) {
                return ScriptAnalysisResult.ScriptPurpose.BUILD;
            }
            if (command.contains("test")) {
                return ScriptAnalysisResult.ScriptPurpose.TEST;
            }
            if (command.contains("deploy")) {
                return ScriptAnalysisResult.ScriptPurpose.DEPLOYMENT;
            }
        }
        
        // Check dependencies
        if (!result.getDependencies().isEmpty()) {
            return ScriptAnalysisResult.ScriptPurpose.SETUP;
        }
        
        return ScriptAnalysisResult.ScriptPurpose.UTILITY;
    }
    
    private void detectEmbeddedLanguages(ScriptAnalysisResult result) {
        // Ensure shell is always listed as an embedded language
        if (!result.getEmbeddedLanguages().contains("Shell")) {
            result.addEmbeddedLanguage("Shell");
        }
    }
    
    private int countOccurrences(String str, char ch) {
        int count = 0;
        for (char c : str.toCharArray()) {
            if (c == ch) count++;
        }
        return count;
    }
}