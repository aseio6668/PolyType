package com.polytype.migrator.scripts.parsers;

import com.polytype.migrator.scripts.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Windows batch scripts (.bat, .cmd files).
 * Analyzes batch script structure, commands, dependencies, and purpose.
 */
public class BatchScriptParser implements ScriptParser {
    
    @Override
    public ScriptAnalyzer.ScriptType getSupportedType() {
        return ScriptAnalyzer.ScriptType.BATCH_SCRIPT;
    }
    
    @Override
    public boolean canHandle(Path scriptFile) {
        String fileName = scriptFile.getFileName().toString().toLowerCase();
        return fileName.endsWith(".bat") || fileName.endsWith(".cmd");
    }
    
    // Pattern for variable assignments (SET command)
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(
        "^\\s*(?:set\\s+)?([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*(.+)$", Pattern.CASE_INSENSITIVE
    );
    
    // Pattern for label definitions
    private static final Pattern LABEL_PATTERN = Pattern.compile("^\\s*:([A-Za-z_][A-Za-z0-9_]*)\\s*$");
    
    // Pattern for commands that indicate build processes
    private static final Pattern BUILD_COMMAND_PATTERN = Pattern.compile(
        "\\b(cl|link|msbuild|devenv|nmake|javac|mvn|gradle|npm|yarn|cargo|go|python|pip)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern for dependency management commands
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
        "\\b(choco|scoop|pip|npm|yarn|cargo|go get)\\s+(?:install\\s+)?([^\\s]+)",
        Pattern.CASE_INSENSITIVE
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
    
    // Pattern for conditional statements
    private static final Pattern IF_PATTERN = Pattern.compile(
        "^\\s*if\\s+(.+?)\\s+(.+)$", Pattern.CASE_INSENSITIVE
    );

    @Override
    public ScriptAnalysisResult parseScript(Path scriptFile) throws IOException {
        ScriptAnalysisResult result = new ScriptAnalysisResult();
        result.setScriptType(ScriptAnalyzer.ScriptType.BATCH_SCRIPT);
        result.setFilePath(scriptFile.toString());
        result.setLanguage("Batch");
        result.addEmbeddedLanguage("Batch");
        
        List<String> lines = Files.readAllLines(scriptFile);
        if (lines.isEmpty()) {
            result.setPurpose(ScriptAnalysisResult.ScriptPurpose.UNKNOWN);
            return result;
        }
        
        String fileName = scriptFile.getFileName().toString().toLowerCase();
        analyzeFileName(fileName, result);
        
        // Parse content line by line
        String currentLabel = null;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Skip empty lines
            if (trimmedLine.isEmpty()) continue;
            
            // Handle comments (REM or ::)
            if (trimmedLine.toUpperCase().startsWith("REM ") || trimmedLine.startsWith("::")) {
                analyzeComment(trimmedLine, result);
                continue;
            }
            
            // Skip @echo off and similar directives
            if (trimmedLine.equalsIgnoreCase("@echo off") || 
                trimmedLine.toLowerCase().startsWith("setlocal")) {
                result.addCommand(trimmedLine);
                continue;
            }
            
            // Parse variable assignments (SET command)
            Matcher variableMatcher = VARIABLE_PATTERN.matcher(trimmedLine);
            if (variableMatcher.find()) {
                String varName = variableMatcher.group(1);
                String varValue = variableMatcher.group(2);
                
                // Clean quotes from value
                varValue = varValue.replaceAll("^\"|\"$", "");
                
                result.addVariable(varName, varValue);
                analyzeConfigVariable(varName, varValue, result);
                result.addCommand(trimmedLine);
                continue;
            }
            
            // Parse label definitions
            Matcher labelMatcher = LABEL_PATTERN.matcher(trimmedLine);
            if (labelMatcher.find()) {
                currentLabel = labelMatcher.group(1);
                result.addMetadata("label_" + currentLabel, "defined");
                result.addBuildTarget(currentLabel);
                continue;
            }
            
            // Parse conditional statements
            Matcher ifMatcher = IF_PATTERN.matcher(trimmedLine);
            if (ifMatcher.find()) {
                String condition = ifMatcher.group(1);
                String action = ifMatcher.group(2);
                result.addMetadata("conditional", condition);
                result.addCommand(trimmedLine);
                
                // Analyze the action part for embedded commands
                analyzeCommand(action, result, currentLabel);
                continue;
            }
            
            // Analyze general commands
            analyzeCommand(trimmedLine, result, currentLabel);
        }
        
        // Determine overall purpose if not already set
        if (result.getPurpose() == ScriptAnalysisResult.ScriptPurpose.UNKNOWN) {
            result.setPurpose(inferPurposeFromContent(result));
        }
        
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
        } else if (fileName.contains("clean")) {
            result.setPurpose(ScriptAnalysisResult.ScriptPurpose.CLEANUP);
        }
    }
    
    private void analyzeComment(String comment, ScriptAnalysisResult result) {
        String content = comment.toUpperCase();
        
        // Remove REM or :: prefix
        if (content.startsWith("REM ")) {
            content = content.substring(4).trim();
        } else if (content.startsWith("::")) {
            content = content.substring(2).trim();
        }
        
        if (BUILD_PURPOSE_PATTERN.matcher(content).find()) {
            result.setPurpose(ScriptAnalysisResult.ScriptPurpose.BUILD);
        } else if (TEST_PURPOSE_PATTERN.matcher(content).find()) {
            result.setPurpose(ScriptAnalysisResult.ScriptPurpose.TEST);
        } else if (SETUP_PURPOSE_PATTERN.matcher(content).find()) {
            result.setPurpose(ScriptAnalysisResult.ScriptPurpose.SETUP);
        }
        
        // Look for version information
        if (content.contains("VERSION") && content.matches(".*\\d+\\.\\d+.*")) {
            Pattern versionPattern = Pattern.compile("(\\d+\\.\\d+[.\\d]*)");
            Matcher matcher = versionPattern.matcher(content);
            if (matcher.find()) {
                result.setVersion(matcher.group(1));
            }
        }
    }
    
    private void analyzeConfigVariable(String varName, String varValue, ScriptAnalysisResult result) {
        String name = varName.toUpperCase();
        
        // Common configuration variables
        if (name.contains("VERSION")) {
            result.setVersion(varValue);
        } else if (name.contains("JAVA_HOME") || name.contains("JDK")) {
            result.addEmbeddedLanguage("Java");
            result.addDependency("Java JDK");
        } else if (name.contains("PYTHON") || name.contains("PIP")) {
            result.addEmbeddedLanguage("Python");
            result.addDependency("Python");
        } else if (name.contains("NODE") || name.contains("NPM")) {
            result.addEmbeddedLanguage("JavaScript");
            result.addDependency("Node.js");
        } else if (name.contains("CARGO") || name.contains("RUST")) {
            result.addEmbeddedLanguage("Rust");
            result.addDependency("Rust");
        } else if (name.contains("GO")) {
            result.addEmbeddedLanguage("Go");
            result.addDependency("Go");
        } else if (name.contains("MSBUILD") || name.contains("DEVENV")) {
            result.addEmbeddedLanguage("C#");
            result.addDependency("Visual Studio");
        } else if (name.contains("CL") || name.contains("LINK")) {
            result.addEmbeddedLanguage("C++");
            result.addDependency("MSVC");
        }
        
        // Build targets
        if (name.contains("TARGET") || name.contains("OUTPUT") || name.contains("DEST")) {
            result.addBuildTarget(varValue);
        }
        
        // Project name or title
        if (name.contains("PROJECT") || name.contains("APP") || name.contains("NAME")) {
            result.addMetadata("project_name", varValue);
        }
    }
    
    private void analyzeCommand(String line, ScriptAnalysisResult result, String currentLabel) {
        result.addCommand(line);
        String upperLine = line.toUpperCase();
        
        // Look for build commands
        Matcher buildMatcher = BUILD_COMMAND_PATTERN.matcher(line);
        if (buildMatcher.find()) {
            String command = buildMatcher.group(1).toLowerCase();
            
            switch (command) {
                case "cl":
                case "link":
                case "nmake":
                    result.addEmbeddedLanguage("C++");
                    result.addDependency("MSVC");
                    break;
                case "msbuild":
                case "devenv":
                    result.addEmbeddedLanguage("C#");
                    result.addDependency("Visual Studio");
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
            String packageManager = depMatcher.group(1).toLowerCase();
            String dependency = depMatcher.group(2);
            
            result.addDependency(dependency);
            
            switch (packageManager) {
                case "choco":
                case "scoop":
                    result.addDependency("Package Manager");
                    break;
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
        
        // Look for file operations that suggest build processes
        if (upperLine.contains("COPY") || upperLine.contains("XCOPY") || upperLine.contains("ROBOCOPY")) {
            result.addMetadata("file_operations", "true");
        }
        
        // Look for test commands
        if (upperLine.contains("TEST") || upperLine.contains("SPEC") || upperLine.contains("CHECK")) {
            if (result.getPurpose() == ScriptAnalysisResult.ScriptPurpose.UNKNOWN) {
                result.setPurpose(ScriptAnalysisResult.ScriptPurpose.TEST);
            }
        }
        
        // Look for deployment commands
        if (upperLine.contains("DEPLOY") || upperLine.contains("PUBLISH") || upperLine.contains("RELEASE")) {
            if (result.getPurpose() == ScriptAnalysisResult.ScriptPurpose.UNKNOWN) {
                result.setPurpose(ScriptAnalysisResult.ScriptPurpose.DEPLOYMENT);
            }
        }
        
        // Look for cleanup commands
        if (upperLine.contains("DEL") || upperLine.contains("RMDIR") || upperLine.contains("CLEAN")) {
            if (result.getPurpose() == ScriptAnalysisResult.ScriptPurpose.UNKNOWN) {
                result.setPurpose(ScriptAnalysisResult.ScriptPurpose.CLEANUP);
            }
        }
        
        // Look for environment setup
        if (upperLine.contains("PATH") || upperLine.contains("CLASSPATH") || upperLine.contains("ENV")) {
            result.addMetadata("environment_setup", "true");
            if (result.getPurpose() == ScriptAnalysisResult.ScriptPurpose.UNKNOWN) {
                result.setPurpose(ScriptAnalysisResult.ScriptPurpose.SETUP);
            }
        }
        
        // Look for service/application control
        if (upperLine.contains("START") || upperLine.contains("STOP") || upperLine.contains("RESTART")) {
            if (result.getPurpose() == ScriptAnalysisResult.ScriptPurpose.UNKNOWN) {
                result.setPurpose(ScriptAnalysisResult.ScriptPurpose.RUNTIME);
            }
        }
    }
    
    private ScriptAnalysisResult.ScriptPurpose inferPurposeFromContent(ScriptAnalysisResult result) {
        // Check commands for purpose hints
        for (String command : result.getCommands()) {
            String upperCommand = command.toUpperCase();
            
            if (BUILD_COMMAND_PATTERN.matcher(command).find()) {
                return ScriptAnalysisResult.ScriptPurpose.BUILD;
            }
            if (upperCommand.contains("TEST")) {
                return ScriptAnalysisResult.ScriptPurpose.TEST;
            }
            if (upperCommand.contains("DEPLOY")) {
                return ScriptAnalysisResult.ScriptPurpose.DEPLOYMENT;
            }
            if (upperCommand.contains("CLEAN") || upperCommand.contains("DEL")) {
                return ScriptAnalysisResult.ScriptPurpose.CLEANUP;
            }
        }
        
        // Check dependencies
        if (!result.getDependencies().isEmpty()) {
            return ScriptAnalysisResult.ScriptPurpose.SETUP;
        }
        
        // Check metadata
        if (result.getMetadata().containsKey("file_operations")) {
            return ScriptAnalysisResult.ScriptPurpose.BUILD;
        }
        
        if (result.getMetadata().containsKey("environment_setup")) {
            return ScriptAnalysisResult.ScriptPurpose.SETUP;
        }
        
        return ScriptAnalysisResult.ScriptPurpose.UTILITY;
    }
}