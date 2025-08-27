package com.polytype.migrator.scripts.parsers;

import com.polytype.migrator.scripts.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Gradle build scripts (build.gradle, build.gradle.kts).
 * Analyzes Gradle configuration, dependencies, tasks, and plugins.
 */
public class GradleParser implements ScriptParser {
    
    @Override
    public ScriptAnalyzer.ScriptType getSupportedType() {
        return ScriptAnalyzer.ScriptType.BUILD_GRADLE;
    }
    
    @Override
    public boolean canHandle(Path scriptFile) {
        String fileName = scriptFile.getFileName().toString().toLowerCase();
        return fileName.equals("build.gradle") || fileName.equals("build.gradle.kts") ||
               fileName.equals("settings.gradle") || fileName.equals("settings.gradle.kts");
    }
    
    // Pattern for plugin declarations
    private static final Pattern PLUGIN_PATTERN = Pattern.compile(
        "(?:apply\\s+plugin:\\s*['\"]([^'\"]+)['\"]|id\\s*['\"]([^'\"]+)['\"])"
    );
    
    // Pattern for dependencies
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
        "(implementation|compile|testImplementation|testCompile|api|runtimeOnly)\\s+['\"]([^'\"]+)['\"]"
    );
    
    // Pattern for version declarations
    private static final Pattern VERSION_PATTERN = Pattern.compile(
        "version\\s*=\\s*['\"]([^'\"]+)['\"]"
    );
    
    // Pattern for group declarations
    private static final Pattern GROUP_PATTERN = Pattern.compile(
        "group\\s*=\\s*['\"]([^'\"]+)['\"]"
    );
    
    // Pattern for task definitions
    private static final Pattern TASK_PATTERN = Pattern.compile(
        "task\\s+(\\w+)(?:\\s*\\(.*?\\))?\\s*\\{"
    );
    
    // Pattern for repository declarations
    private static final Pattern REPOSITORY_PATTERN = Pattern.compile(
        "(mavenCentral|jcenter|gradlePluginPortal|maven)\\s*(?:\\{|\\()"
    );

    @Override
    public ScriptAnalysisResult parseScript(Path scriptFile) throws IOException {
        ScriptAnalysisResult result = new ScriptAnalysisResult();
        result.setScriptType(ScriptAnalyzer.ScriptType.BUILD_GRADLE);
        result.setFilePath(scriptFile.toString());
        result.setLanguage("Groovy");
        result.setPurpose(ScriptAnalysisResult.ScriptPurpose.BUILD);
        result.addEmbeddedLanguage("Java");
        result.addEmbeddedLanguage("Groovy");
        
        // Determine if it's Kotlin DSL
        String fileName = scriptFile.getFileName().toString();
        if (fileName.endsWith(".kts")) {
            result.setLanguage("Kotlin");
            result.addEmbeddedLanguage("Kotlin");
        }
        
        List<String> lines = Files.readAllLines(scriptFile);
        if (lines.isEmpty()) {
            return result;
        }
        
        String content = String.join("\n", lines);
        
        // Parse plugins
        parsePlugins(content, result);
        
        // Parse dependencies
        parseDependencies(content, result);
        
        // Parse project information
        parseProjectInfo(content, result);
        
        // Parse tasks
        parseTasks(content, result);
        
        // Parse repositories
        parseRepositories(content, result);
        
        // Detect embedded languages from plugins and dependencies
        detectEmbeddedLanguagesFromPlugins(result);
        
        return result;
    }
    
    private void parsePlugins(String content, ScriptAnalysisResult result) {
        Matcher matcher = PLUGIN_PATTERN.matcher(content);
        while (matcher.find()) {
            String plugin = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            result.addDependency("Plugin: " + plugin);
            result.addMetadata("plugin_" + plugin.replace("-", "_"), "applied");
            
            // Analyze plugin for language hints
            analyzePlugin(plugin, result);
        }
    }
    
    private void analyzePlugin(String plugin, ScriptAnalysisResult result) {
        switch (plugin) {
            case "java":
            case "java-library":
            case "application":
                result.addEmbeddedLanguage("Java");
                break;
            case "kotlin":
            case "kotlin-android":
            case "org.jetbrains.kotlin.jvm":
                result.addEmbeddedLanguage("Kotlin");
                break;
            case "scala":
                result.addEmbeddedLanguage("Scala");
                break;
            case "groovy":
                result.addEmbeddedLanguage("Groovy");
                break;
            case "android":
            case "com.android.application":
            case "com.android.library":
                result.addEmbeddedLanguage("Java");
                result.addEmbeddedLanguage("Android");
                result.addDependency("Android SDK");
                break;
            case "war":
                result.addMetadata("packaging", "war");
                result.addEmbeddedLanguage("Java");
                break;
            case "spring-boot":
            case "org.springframework.boot":
                result.addEmbeddedLanguage("Java");
                result.addDependency("Spring Boot");
                break;
            case "maven-publish":
            case "signing":
                result.addMetadata("publishing", "enabled");
                break;
        }
    }
    
    private void parseDependencies(String content, ScriptAnalysisResult result) {
        Matcher matcher = DEPENDENCY_PATTERN.matcher(content);
        while (matcher.find()) {
            String scope = matcher.group(1);
            String dependency = matcher.group(2);
            
            result.addDependency(dependency);
            result.addMetadata("dependency_" + scope, dependency);
            
            // Analyze dependency for language/framework hints
            analyzeDependency(dependency, result);
        }
    }
    
    private void analyzeDependency(String dependency, ScriptAnalysisResult result) {
        String[] parts = dependency.split(":");
        if (parts.length >= 2) {
            String group = parts[0];
            String artifact = parts[1];
            
            // Spring Framework
            if (group.startsWith("org.springframework")) {
                result.addEmbeddedLanguage("Java");
                result.addDependency("Spring Framework");
            }
            // Kotlin libraries
            else if (group.startsWith("org.jetbrains.kotlin")) {
                result.addEmbeddedLanguage("Kotlin");
            }
            // Scala libraries
            else if (group.startsWith("org.scala-lang")) {
                result.addEmbeddedLanguage("Scala");
            }
            // Android libraries
            else if (group.startsWith("com.android") || group.startsWith("androidx")) {
                result.addEmbeddedLanguage("Android");
                result.addDependency("Android SDK");
            }
            // Testing frameworks
            else if (artifact.contains("junit") || artifact.contains("testng") || 
                     artifact.contains("spock") || artifact.contains("kotest")) {
                result.addDependency("Testing Framework");
            }
            // Web frameworks
            else if (artifact.contains("jersey") || artifact.contains("jax-rs") ||
                     artifact.contains("servlet") || artifact.contains("jetty")) {
                result.addDependency("Web Framework");
            }
        }
    }
    
    private void parseProjectInfo(String content, ScriptAnalysisResult result) {
        Matcher versionMatcher = VERSION_PATTERN.matcher(content);
        if (versionMatcher.find()) {
            result.setVersion(versionMatcher.group(1));
        }
        
        Matcher groupMatcher = GROUP_PATTERN.matcher(content);
        if (groupMatcher.find()) {
            result.addMetadata("group", groupMatcher.group(1));
        }
        
        // Look for project name
        Pattern projectNamePattern = Pattern.compile("rootProject\\.name\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher projectNameMatcher = projectNamePattern.matcher(content);
        if (projectNameMatcher.find()) {
            result.addMetadata("project_name", projectNameMatcher.group(1));
        }
    }
    
    private void parseTasks(String content, ScriptAnalysisResult result) {
        Matcher matcher = TASK_PATTERN.matcher(content);
        while (matcher.find()) {
            String taskName = matcher.group(1);
            result.addBuildTarget(taskName);
            result.addMetadata("task_" + taskName, "defined");
        }
        
        // Look for common task configurations
        if (content.contains("test {") || content.contains("test(")) {
            result.addBuildTarget("test");
            result.addMetadata("test_configured", "true");
        }
        
        if (content.contains("jar {") || content.contains("jar(")) {
            result.addBuildTarget("jar");
            result.addMetadata("jar_configured", "true");
        }
        
        if (content.contains("bootJar") || content.contains("bootWar")) {
            result.addBuildTarget("spring-boot-package");
            result.addDependency("Spring Boot");
        }
    }
    
    private void parseRepositories(String content, ScriptAnalysisResult result) {
        Matcher matcher = REPOSITORY_PATTERN.matcher(content);
        Set<String> repositories = new HashSet<>();
        
        while (matcher.find()) {
            String repo = matcher.group(1);
            repositories.add(repo);
        }
        
        for (String repo : repositories) {
            result.addDependency("Repository: " + repo);
            result.addMetadata("repository_" + repo, "configured");
        }
    }
    
    private void detectEmbeddedLanguagesFromPlugins(ScriptAnalysisResult result) {
        Map<String, String> metadata = result.getMetadata();
        
        // Default assumption is Java if no specific language plugins detected
        boolean hasLanguagePlugin = false;
        
        for (String key : metadata.keySet()) {
            if (key.startsWith("plugin_")) {
                hasLanguagePlugin = true;
                break;
            }
        }
        
        // If we have Java-like dependencies but no explicit language plugins,
        // assume Java is the primary language
        if (!hasLanguagePlugin && !result.getDependencies().isEmpty()) {
            boolean hasJavaDependencies = result.getDependencies().stream()
                .anyMatch(dep -> dep.contains("org.") || dep.contains("com.") || 
                         dep.contains("junit") || dep.contains("spring"));
            
            if (hasJavaDependencies) {
                result.addEmbeddedLanguage("Java");
            }
        }
        
        // Check for multi-module project indicators
        if (result.getMetadata().containsKey("project_name") || 
            content.contains("subprojects") || content.contains("allprojects")) {
            result.addMetadata("multi_module", "true");
        }
    }
}