package com.polytype.migrator.scripts.parsers;

import com.polytype.migrator.scripts.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for package.json files (Node.js/JavaScript projects).
 * Extracts dependencies, scripts, configuration, and project metadata.
 */
public class PackageJsonParser implements ScriptParser {
    
    private static final Pattern NAME_PATTERN = Pattern.compile(
        "\"name\"\\s*:\\s*\"([^\"]+)\""
    );
    
    private static final Pattern VERSION_PATTERN = Pattern.compile(
        "\"version\"\\s*:\\s*\"([^\"]+)\""
    );
    
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
        "\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\""
    );
    
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
        "\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\""
    );
    
    @Override
    public ScriptAnalysisResult parseScript(Path scriptFile) throws IOException {
        ScriptAnalysisResult result = new ScriptAnalysisResult();
        result.setScriptType(ScriptAnalyzer.ScriptType.PACKAGE_JSON);
        result.setFilePath(scriptFile.toString());
        result.setLanguage("JavaScript");
        result.setPurpose(ScriptAnalysisResult.ScriptPurpose.DEPENDENCY_MANAGEMENT);
        
        String content = Files.readString(scriptFile);
        
        // Parse basic project info
        Matcher nameMatcher = NAME_PATTERN.matcher(content);
        if (nameMatcher.find()) {
            result.addConfiguration("project_name", nameMatcher.group(1));
            result.addMetadata("name", nameMatcher.group(1));
        }
        
        Matcher versionMatcher = VERSION_PATTERN.matcher(content);
        if (versionMatcher.find()) {
            result.setVersion(versionMatcher.group(1));
            result.addMetadata("version", versionMatcher.group(1));
        }
        
        // Parse scripts section
        String scriptsSection = extractSection(content, "scripts");
        if (!scriptsSection.isEmpty()) {
            result.setPurpose(ScriptAnalysisResult.ScriptPurpose.BUILD);
            parseScripts(scriptsSection, result);
        }
        
        // Parse dependencies
        String dependenciesSection = extractSection(content, "dependencies");
        if (!dependenciesSection.isEmpty()) {
            parseDependencies(dependenciesSection, result, false);
        }
        
        // Parse devDependencies
        String devDependenciesSection = extractSection(content, "devDependencies");
        if (!devDependenciesSection.isEmpty()) {
            parseDependencies(devDependenciesSection, result, true);
        }
        
        // Parse peerDependencies
        String peerDependenciesSection = extractSection(content, "peerDependencies");
        if (!peerDependenciesSection.isEmpty()) {
            parseDependencies(peerDependenciesSection, result, false);
        }
        
        // Detect framework and environment
        detectFrameworkAndEnvironment(result);
        
        // Add Node.js specific configuration
        result.addConfiguration("package_manager", detectPackageManager(scriptFile));
        result.addConfiguration("node_version", extractNodeVersion(content));
        result.addEmbeddedLanguage("JavaScript");
        
        // Detect TypeScript
        if (result.getDependencies().stream().anyMatch(dep -> dep.contains("typescript"))) {
            result.addEmbeddedLanguage("TypeScript");
            result.addMetadata("typescript", "true");
        }
        
        return result;
    }
    
    private String extractSection(String content, String sectionName) {
        Pattern sectionPattern = Pattern.compile(
            "\"" + sectionName + "\"\\s*:\\s*\\{([^}]*)\\}",
            Pattern.DOTALL
        );
        
        Matcher matcher = sectionPattern.matcher(content);
        return matcher.find() ? matcher.group(1) : "";
    }
    
    private void parseScripts(String scriptsSection, ScriptAnalysisResult result) {
        Matcher scriptMatcher = SCRIPT_PATTERN.matcher(scriptsSection);
        
        while (scriptMatcher.find()) {
            String scriptName = scriptMatcher.group(1);
            String scriptCommand = scriptMatcher.group(2);
            
            result.addCommand(scriptName + ": " + scriptCommand);
            result.addBuildTarget(scriptName);
            
            // Analyze script commands for embedded languages and tools
            analyzeScriptCommand(scriptCommand, result);
        }
    }
    
    private void parseDependencies(String dependenciesSection, ScriptAnalysisResult result, boolean isDev) {
        Matcher depMatcher = DEPENDENCY_PATTERN.matcher(dependenciesSection);
        
        while (depMatcher.find()) {
            String depName = depMatcher.group(1);
            String depVersion = depMatcher.group(2);
            
            String dependency = depName + "@" + depVersion;
            if (isDev) {
                dependency += " (dev)";
            }
            
            result.addDependency(dependency);
        }
    }
    
    private void analyzeScriptCommand(String command, ScriptAnalysisResult result) {
        // Detect build tools
        if (command.contains("webpack")) {
            result.addMetadata("bundler", "webpack");
            result.addEmbeddedLanguage("JavaScript");
        } else if (command.contains("vite")) {
            result.addMetadata("bundler", "vite");
        } else if (command.contains("rollup")) {
            result.addMetadata("bundler", "rollup");
        }
        
        // Detect test frameworks
        if (command.contains("jest")) {
            result.addMetadata("test_framework", "jest");
        } else if (command.contains("mocha")) {
            result.addMetadata("test_framework", "mocha");
        } else if (command.contains("vitest")) {
            result.addMetadata("test_framework", "vitest");
        }
        
        // Detect TypeScript
        if (command.contains("tsc") || command.contains("typescript")) {
            result.addEmbeddedLanguage("TypeScript");
            result.addMetadata("typescript", "true");
        }
        
        // Detect linting tools
        if (command.contains("eslint")) {
            result.addMetadata("linter", "eslint");
        } else if (command.contains("prettier")) {
            result.addMetadata("formatter", "prettier");
        }
    }
    
    private void detectFrameworkAndEnvironment(ScriptAnalysisResult result) {
        // Detect popular frameworks based on dependencies
        for (String dependency : result.getDependencies()) {
            if (dependency.contains("react")) {
                result.addMetadata("framework", "React");
                result.addEmbeddedLanguage("JSX");
            } else if (dependency.contains("vue")) {
                result.addMetadata("framework", "Vue.js");
            } else if (dependency.contains("angular")) {
                result.addMetadata("framework", "Angular");
                result.addEmbeddedLanguage("TypeScript");
            } else if (dependency.contains("express")) {
                result.addMetadata("framework", "Express.js");
                result.addMetadata("environment", "Node.js");
            } else if (dependency.contains("next")) {
                result.addMetadata("framework", "Next.js");
                result.addEmbeddedLanguage("JSX");
            } else if (dependency.contains("nuxt")) {
                result.addMetadata("framework", "Nuxt.js");
            } else if (dependency.contains("svelte")) {
                result.addMetadata("framework", "Svelte");
            }
        }
    }
    
    private String detectPackageManager(Path packageJsonPath) {
        Path projectRoot = packageJsonPath.getParent();
        
        // Check for lock files to determine package manager
        if (Files.exists(projectRoot.resolve("yarn.lock"))) {
            return "yarn";
        } else if (Files.exists(projectRoot.resolve("pnpm-lock.yaml"))) {
            return "pnpm";
        } else if (Files.exists(projectRoot.resolve("package-lock.json"))) {
            return "npm";
        }
        
        return "npm"; // Default
    }
    
    private String extractNodeVersion(String content) {
        Pattern nodeVersionPattern = Pattern.compile(
            "\"engines\"\\s*:\\s*\\{[^}]*\"node\"\\s*:\\s*\"([^\"]+)\""
        );
        
        Matcher matcher = nodeVersionPattern.matcher(content);
        return matcher.find() ? matcher.group(1) : "latest";
    }
    
    @Override
    public ScriptAnalyzer.ScriptType getSupportedType() {
        return ScriptAnalyzer.ScriptType.PACKAGE_JSON;
    }
    
    @Override
    public boolean canHandle(Path scriptFile) {
        return "package.json".equals(scriptFile.getFileName().toString());
    }
}