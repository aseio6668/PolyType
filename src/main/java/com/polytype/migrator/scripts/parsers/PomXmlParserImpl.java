package com.polytype.migrator.scripts.parsers;

import com.polytype.migrator.scripts.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Maven pom.xml files.
 * Extracts project information, dependencies, plugins, and build configuration.
 */
public class PomXmlParserImpl implements ScriptParser {
    
    private static final Pattern GROUP_ID_PATTERN = Pattern.compile(
        "<groupId>([^<]+)</groupId>"
    );
    
    private static final Pattern ARTIFACT_ID_PATTERN = Pattern.compile(
        "<artifactId>([^<]+)</artifactId>"
    );
    
    private static final Pattern VERSION_PATTERN = Pattern.compile(
        "<version>([^<]+)</version>"
    );
    
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
        "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>\\s*<version>([^<]+)</version>(?:\\s*<scope>([^<]+)</scope>)?[^<]*</dependency>",
        Pattern.DOTALL
    );
    
    private static final Pattern PLUGIN_PATTERN = Pattern.compile(
        "<plugin>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>(?:\\s*<version>([^<]+)</version>)?[^<]*</plugin>",
        Pattern.DOTALL
    );
    
    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
        "<([^/>]+)>([^<]+)</\\1>"
    );
    
    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile(
        "<(?:maven\\.compiler\\.(?:source|target)|java\\.version)>([^<]+)</"
    );
    
    @Override
    public ScriptAnalysisResult parseScript(Path scriptFile) throws IOException {
        ScriptAnalysisResult result = new ScriptAnalysisResult();
        result.setScriptType(ScriptAnalyzer.ScriptType.POM_XML);
        result.setFilePath(scriptFile.toString());
        result.setLanguage("Java");
        result.setPurpose(ScriptAnalysisResult.ScriptPurpose.DEPENDENCY_MANAGEMENT);
        result.addEmbeddedLanguage("Java");
        
        String content = Files.readString(scriptFile);
        
        // Parse project coordinates
        Matcher groupIdMatcher = GROUP_ID_PATTERN.matcher(content);
        if (groupIdMatcher.find()) {
            String groupId = groupIdMatcher.group(1);
            result.addConfiguration("group_id", groupId);
            result.addMetadata("groupId", groupId);
        }
        
        Matcher artifactIdMatcher = ARTIFACT_ID_PATTERN.matcher(content);
        if (artifactIdMatcher.find()) {
            String artifactId = artifactIdMatcher.group(1);
            result.addConfiguration("artifact_id", artifactId);
            result.addMetadata("artifactId", artifactId);
            result.addMetadata("name", artifactId);
        }
        
        Matcher versionMatcher = VERSION_PATTERN.matcher(content);
        if (versionMatcher.find()) {
            String version = versionMatcher.group(1);
            result.setVersion(version);
            result.addMetadata("version", version);
        }
        
        // Parse dependencies
        Matcher dependencyMatcher = DEPENDENCY_PATTERN.matcher(content);
        while (dependencyMatcher.find()) {
            String depGroupId = dependencyMatcher.group(1);
            String depArtifactId = dependencyMatcher.group(2);
            String depVersion = dependencyMatcher.group(3);
            String scope = dependencyMatcher.group(4);
            
            String dependency = depGroupId + ":" + depArtifactId + ":" + depVersion;
            if (scope != null) {
                dependency += " (" + scope + ")";
            }
            
            result.addDependency(dependency);
            
            // Detect frameworks and libraries
            detectFrameworksAndLibraries(depGroupId, depArtifactId, result);
        }
        
        // Parse plugins
        Matcher pluginMatcher = PLUGIN_PATTERN.matcher(content);
        while (pluginMatcher.find()) {
            String pluginGroupId = pluginMatcher.group(1);
            String pluginArtifactId = pluginMatcher.group(2);
            String pluginVersion = pluginMatcher.group(3);
            
            String plugin = pluginGroupId + ":" + pluginArtifactId;
            if (pluginVersion != null) {
                plugin += ":" + pluginVersion;
            }
            
            result.addConfiguration("plugin", plugin);
            
            // Detect build capabilities
            if ("maven-compiler-plugin".equals(pluginArtifactId)) {
                result.setPurpose(ScriptAnalysisResult.ScriptPurpose.BUILD);
                result.addBuildTarget("compile");
            } else if ("maven-surefire-plugin".equals(pluginArtifactId)) {
                result.addBuildTarget("test");
                result.addMetadata("test_framework", "surefire");
            } else if ("maven-failsafe-plugin".equals(pluginArtifactId)) {
                result.addBuildTarget("integration-test");
            } else if ("maven-assembly-plugin".equals(pluginArtifactId) || 
                      "maven-shade-plugin".equals(pluginArtifactId)) {
                result.addBuildTarget("package");
            }
        }
        
        // Parse Java version from properties
        Matcher javaVersionMatcher = JAVA_VERSION_PATTERN.matcher(content);
        if (javaVersionMatcher.find()) {
            String javaVersion = javaVersionMatcher.group(1);
            result.addConfiguration("java_version", javaVersion);
            result.addMetadata("java.version", javaVersion);
        }
        
        // Parse properties section
        String propertiesSection = extractSection(content, "properties");
        if (!propertiesSection.isEmpty()) {
            parseProperties(propertiesSection, result);
        }
        
        // Detect packaging type
        Pattern packagingPattern = Pattern.compile("<packaging>([^<]+)</packaging>");
        Matcher packagingMatcher = packagingPattern.matcher(content);
        if (packagingMatcher.find()) {
            String packaging = packagingMatcher.group(1);
            result.addConfiguration("packaging", packaging);
            
            if ("war".equals(packaging)) {
                result.addMetadata("application_type", "web");
                result.addEmbeddedLanguage("JSP");
            } else if ("ear".equals(packaging)) {
                result.addMetadata("application_type", "enterprise");
            }
        }
        
        // Add Maven-specific configuration
        result.addConfiguration("build_system", "maven");
        result.addConfiguration("package_manager", "maven");
        
        return result;
    }
    
    private String extractSection(String content, String sectionName) {
        Pattern sectionPattern = Pattern.compile(
            "<" + sectionName + ">([^<]*(?:<(?!/?)" + sectionName + ")[^<]*)*)</" + sectionName + ">",
            Pattern.DOTALL
        );
        
        Matcher matcher = sectionPattern.matcher(content);
        return matcher.find() ? matcher.group(1) : "";
    }
    
    private void parseProperties(String propertiesSection, ScriptAnalysisResult result) {
        Matcher propertyMatcher = PROPERTY_PATTERN.matcher(propertiesSection);
        
        while (propertyMatcher.find()) {
            String propertyName = propertyMatcher.group(1);
            String propertyValue = propertyMatcher.group(2);
            
            result.addVariable(propertyName, propertyValue);
            
            // Special handling for common properties
            if (propertyName.contains("version")) {
                result.addConfiguration("property_" + propertyName, propertyValue);
            }
        }
    }
    
    private void detectFrameworksAndLibraries(String groupId, String artifactId, ScriptAnalysisResult result) {
        // Spring Framework
        if (groupId.startsWith("org.springframework")) {
            result.addMetadata("framework", "Spring");
            if (artifactId.contains("boot")) {
                result.addMetadata("framework_type", "Spring Boot");
            } else if (artifactId.contains("mvc")) {
                result.addMetadata("web_framework", "Spring MVC");
            } else if (artifactId.contains("data")) {
                result.addMetadata("data_framework", "Spring Data");
            }
        }
        
        // Testing frameworks
        if ("junit".equals(groupId) || "org.junit.jupiter".equals(groupId)) {
            result.addMetadata("test_framework", "JUnit");
        } else if ("org.testng".equals(groupId)) {
            result.addMetadata("test_framework", "TestNG");
        } else if ("org.mockito".equals(groupId)) {
            result.addMetadata("mock_framework", "Mockito");
        }
        
        // Database
        if (artifactId.contains("jdbc") || artifactId.contains("mysql") || 
            artifactId.contains("postgresql") || artifactId.contains("h2")) {
            result.addMetadata("database", "SQL");
            result.addEmbeddedLanguage("SQL");
        } else if (artifactId.contains("mongodb") || artifactId.contains("redis")) {
            result.addMetadata("database", "NoSQL");
        }
        
        // Web technologies
        if (artifactId.contains("servlet") || artifactId.contains("jsp")) {
            result.addMetadata("web_technology", "Servlet/JSP");
            result.addEmbeddedLanguage("JSP");
        } else if (artifactId.contains("jax-rs") || artifactId.contains("jersey")) {
            result.addMetadata("web_framework", "JAX-RS");
        }
        
        // Logging
        if (groupId.contains("slf4j") || groupId.contains("logback") || 
            groupId.contains("log4j")) {
            result.addMetadata("logging_framework", artifactId);
        }
    }
    
    @Override
    public ScriptAnalyzer.ScriptType getSupportedType() {
        return ScriptAnalyzer.ScriptType.POM_XML;
    }
    
    @Override
    public boolean canHandle(Path scriptFile) {
        String filename = scriptFile.getFileName().toString();
        return "pom.xml".equals(filename);
    }
}