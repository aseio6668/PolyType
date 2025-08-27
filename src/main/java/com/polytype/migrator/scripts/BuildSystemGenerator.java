package com.polytype.migrator.scripts;

import com.polytype.migrator.core.TargetLanguage;
import java.util.*;

/**
 * Generates build system configuration files for target languages.
 * Creates appropriate build scripts based on project analysis and target language.
 */
public class BuildSystemGenerator {
    
    private final ProjectAnalysisResult projectAnalysis;
    private final TargetLanguage targetLanguage;
    
    public BuildSystemGenerator(ProjectAnalysisResult projectAnalysis, TargetLanguage targetLanguage) {
        this.projectAnalysis = projectAnalysis;
        this.targetLanguage = targetLanguage;
    }
    
    /**
     * Generate build configuration for the target language.
     */
    public Map<String, String> generateBuildConfiguration() {
        Map<String, String> buildConfig = new HashMap<>();
        
        switch (targetLanguage) {
            case JAVA:
                buildConfig.putAll(generateJavaBuildConfig());
                break;
            case PYTHON:
                buildConfig.putAll(generatePythonBuildConfig());
                break;
            case JAVASCRIPT:
                buildConfig.putAll(generateJavaScriptBuildConfig());
                break;
            case RUST:
                buildConfig.putAll(generateRustBuildConfig());
                break;
            case CPP:
                buildConfig.putAll(generateCppBuildConfig());
                break;
            default:
                buildConfig.putAll(generateGenericBuildConfig());
        }
        
        return buildConfig;
    }
    
    private Map<String, String> generateJavaBuildConfig() {
        Map<String, String> config = new HashMap<>();
        
        // Generate Maven pom.xml
        config.put("pom.xml", generateMavenPom());
        
        // Generate Gradle build.gradle
        config.put("build.gradle", generateGradleBuild());
        
        return config;
    }
    
    private String generateMavenPom() {
        StringBuilder pom = new StringBuilder();
        pom.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        pom.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n");
        pom.append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        pom.append("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0\n");
        pom.append("         http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n");
        pom.append("    <modelVersion>4.0.0</modelVersion>\n\n");
        
        pom.append("    <groupId>com.polytype</groupId>\n");
        pom.append("    <artifactId>migrated-project</artifactId>\n");
        pom.append("    <version>1.0.0</version>\n");
        pom.append("    <packaging>jar</packaging>\n\n");
        
        pom.append("    <name>Migrated Project</name>\n");
        pom.append("    <description>Project migrated using PolyType</description>\n\n");
        
        pom.append("    <properties>\n");
        pom.append("        <maven.compiler.source>11</maven.compiler.source>\n");
        pom.append("        <maven.compiler.target>11</maven.compiler.target>\n");
        pom.append("        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n");
        pom.append("    </properties>\n\n");
        
        pom.append("    <dependencies>\n");
        
        // Add common dependencies
        pom.append("        <!-- Testing -->\n");
        pom.append("        <dependency>\n");
        pom.append("            <groupId>org.junit.jupiter</groupId>\n");
        pom.append("            <artifactId>junit-jupiter</artifactId>\n");
        pom.append("            <version>5.9.2</version>\n");
        pom.append("            <scope>test</scope>\n");
        pom.append("        </dependency>\n\n");
        
        // Add dependencies based on project analysis
        for (String dep : projectAnalysis.getDependencies()) {
            String javaDep = mapDependencyToJava(dep);
            if (javaDep != null) {
                pom.append("        ").append(javaDep).append("\n");
            }
        }
        
        pom.append("    </dependencies>\n\n");
        
        pom.append("    <build>\n");
        pom.append("        <plugins>\n");
        pom.append("            <plugin>\n");
        pom.append("                <groupId>org.apache.maven.plugins</groupId>\n");
        pom.append("                <artifactId>maven-compiler-plugin</artifactId>\n");
        pom.append("                <version>3.11.0</version>\n");
        pom.append("            </plugin>\n");
        pom.append("            <plugin>\n");
        pom.append("                <groupId>org.apache.maven.plugins</groupId>\n");
        pom.append("                <artifactId>maven-surefire-plugin</artifactId>\n");
        pom.append("                <version>3.0.0-M9</version>\n");
        pom.append("            </plugin>\n");
        pom.append("        </plugins>\n");
        pom.append("    </build>\n");
        pom.append("</project>\n");
        
        return pom.toString();
    }
    
    private String generateGradleBuild() {
        StringBuilder gradle = new StringBuilder();
        gradle.append("plugins {\n");
        gradle.append("    id 'java'\n");
        gradle.append("    id 'application'\n");
        gradle.append("}\n\n");
        
        gradle.append("group = 'com.polytype'\n");
        gradle.append("version = '1.0.0'\n");
        gradle.append("sourceCompatibility = '11'\n\n");
        
        gradle.append("repositories {\n");
        gradle.append("    mavenCentral()\n");
        gradle.append("}\n\n");
        
        gradle.append("dependencies {\n");
        gradle.append("    // Testing\n");
        gradle.append("    testImplementation 'org.junit.jupiter:junit-jupiter:5.9.2'\n");
        gradle.append("    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'\n\n");
        
        // Add mapped dependencies
        for (String dep : projectAnalysis.getDependencies()) {
            String gradleDep = mapDependencyToGradle(dep);
            if (gradleDep != null) {
                gradle.append("    ").append(gradleDep).append("\n");
            }
        }
        
        gradle.append("}\n\n");
        
        gradle.append("test {\n");
        gradle.append("    useJUnitPlatform()\n");
        gradle.append("}\n\n");
        
        gradle.append("application {\n");
        gradle.append("    mainClass = 'com.polytype.Main'\n");
        gradle.append("}\n");
        
        return gradle.toString();
    }
    
    private Map<String, String> generatePythonBuildConfig() {
        Map<String, String> config = new HashMap<>();
        
        config.put("setup.py", generateSetupPy());
        config.put("pyproject.toml", generatePyprojectToml());
        config.put("requirements.txt", generateRequirementsTxt());
        
        return config;
    }
    
    private String generateSetupPy() {
        return "from setuptools import setup, find_packages\n\n" +
               "setup(\n" +
               "    name='migrated-project',\n" +
               "    version='1.0.0',\n" +
               "    description='Project migrated using PolyType',\n" +
               "    packages=find_packages(),\n" +
               "    python_requires='>=3.8',\n" +
               "    install_requires=[\n" +
               "        # Add dependencies here\n" +
               "    ],\n" +
               "    extras_require={\n" +
               "        'dev': ['pytest', 'black', 'flake8']\n" +
               "    }\n" +
               ")\n";
    }
    
    private String generatePyprojectToml() {
        StringBuilder toml = new StringBuilder();
        toml.append("[build-system]\n");
        toml.append("requires = [\"setuptools>=45\", \"wheel\"]\n");
        toml.append("build-backend = \"setuptools.build_meta\"\n\n");
        
        toml.append("[project]\n");
        toml.append("name = \"migrated-project\"\n");
        toml.append("version = \"1.0.0\"\n");
        toml.append("description = \"Project migrated using PolyType\"\n");
        toml.append("requires-python = \">=3.8\"\n");
        toml.append("dependencies = [\n");
        
        // Map dependencies
        for (String dep : projectAnalysis.getDependencies()) {
            String pythonDep = mapDependencyToPython(dep);
            if (pythonDep != null) {
                toml.append("    \"").append(pythonDep).append("\",\n");
            }
        }
        
        toml.append("]\n\n");
        
        toml.append("[project.optional-dependencies]\n");
        toml.append("dev = [\"pytest\", \"black\", \"flake8\"]\n");
        
        return toml.toString();
    }
    
    private String generateRequirementsTxt() {
        StringBuilder requirements = new StringBuilder();
        requirements.append("# Production dependencies\n");
        
        for (String dep : projectAnalysis.getDependencies()) {
            String pythonDep = mapDependencyToPython(dep);
            if (pythonDep != null) {
                requirements.append(pythonDep).append("\n");
            }
        }
        
        requirements.append("\n# Development dependencies\n");
        requirements.append("pytest>=7.0.0\n");
        requirements.append("black>=22.0.0\n");
        requirements.append("flake8>=5.0.0\n");
        
        return requirements.toString();
    }
    
    private Map<String, String> generateJavaScriptBuildConfig() {
        Map<String, String> config = new HashMap<>();
        
        config.put("package.json", generatePackageJson());
        config.put("webpack.config.js", generateWebpackConfig());
        
        return config;
    }
    
    private String generatePackageJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"name\": \"migrated-project\",\n");
        json.append("  \"version\": \"1.0.0\",\n");
        json.append("  \"description\": \"Project migrated using PolyType\",\n");
        json.append("  \"main\": \"src/index.js\",\n");
        json.append("  \"scripts\": {\n");
        json.append("    \"start\": \"node src/index.js\",\n");
        json.append("    \"build\": \"webpack --mode=production\",\n");
        json.append("    \"dev\": \"webpack --mode=development --watch\",\n");
        json.append("    \"test\": \"jest\"\n");
        json.append("  },\n");
        json.append("  \"dependencies\": {\n");
        
        // Add mapped dependencies
        boolean first = true;
        for (String dep : projectAnalysis.getDependencies()) {
            String jsDep = mapDependencyToJavaScript(dep);
            if (jsDep != null) {
                if (!first) json.append(",\n");
                json.append("    ").append(jsDep);
                first = false;
            }
        }
        json.append("\n  },\n");
        
        json.append("  \"devDependencies\": {\n");
        json.append("    \"webpack\": \"^5.75.0\",\n");
        json.append("    \"webpack-cli\": \"^5.0.1\",\n");
        json.append("    \"jest\": \"^29.3.1\"\n");
        json.append("  },\n");
        json.append("  \"engines\": {\n");
        json.append("    \"node\": \">=14.0.0\"\n");
        json.append("  }\n");
        json.append("}\n");
        
        return json.toString();
    }
    
    private String generateWebpackConfig() {
        return "const path = require('path');\n\n" +
               "module.exports = {\n" +
               "  entry: './src/index.js',\n" +
               "  output: {\n" +
               "    path: path.resolve(__dirname, 'dist'),\n" +
               "    filename: 'bundle.js'\n" +
               "  },\n" +
               "  module: {\n" +
               "    rules: [\n" +
               "      {\n" +
               "        test: /\\.js$/,\n" +
               "        exclude: /node_modules/,\n" +
               "        use: {\n" +
               "          loader: 'babel-loader',\n" +
               "          options: {\n" +
               "            presets: ['@babel/preset-env']\n" +
               "          }\n" +
               "        }\n" +
               "      }\n" +
               "    ]\n" +
               "  }\n" +
               "};\n";
    }
    
    private Map<String, String> generateRustBuildConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("Cargo.toml", generateCargoToml());
        return config;
    }
    
    private String generateCargoToml() {
        StringBuilder toml = new StringBuilder();
        toml.append("[package]\n");
        toml.append("name = \"migrated-project\"\n");
        toml.append("version = \"1.0.0\"\n");
        toml.append("edition = \"2021\"\n");
        toml.append("description = \"Project migrated using PolyType\"\n\n");
        
        toml.append("[dependencies]\n");
        for (String dep : projectAnalysis.getDependencies()) {
            String rustDep = mapDependencyToRust(dep);
            if (rustDep != null) {
                toml.append(rustDep).append("\n");
            }
        }
        
        return toml.toString();
    }
    
    private Map<String, String> generateCppBuildConfig() {
        Map<String, String> config = new HashMap<>();
        
        config.put("CMakeLists.txt", generateCMakeFile());
        config.put("Makefile", generateMakefile());
        
        return config;
    }
    
    private String generateCMakeFile() {
        return "cmake_minimum_required(VERSION 3.10)\n" +
               "project(MigratedProject)\n\n" +
               "set(CMAKE_CXX_STANDARD 17)\n" +
               "set(CMAKE_CXX_STANDARD_REQUIRED ON)\n\n" +
               "# Add executable\n" +
               "add_executable(${PROJECT_NAME} src/main.cpp)\n\n" +
               "# Include directories\n" +
               "target_include_directories(${PROJECT_NAME} PRIVATE include)\n\n" +
               "# Add subdirectories\n" +
               "# add_subdirectory(lib)\n\n" +
               "# Link libraries\n" +
               "# target_link_libraries(${PROJECT_NAME} your_lib)\n";
    }
    
    private String generateMakefile() {
        return "CXX = g++\n" +
               "CXXFLAGS = -Wall -Wextra -std=c++17 -O2\n" +
               "SRCDIR = src\n" +
               "INCDIR = include\n" +
               "BUILDDIR = build\n" +
               "TARGET = $(BUILDDIR)/main\n\n" +
               "SOURCES = $(wildcard $(SRCDIR)/*.cpp)\n" +
               "OBJECTS = $(SOURCES:$(SRCDIR)/%.cpp=$(BUILDDIR)/%.o)\n\n" +
               "all: $(TARGET)\n\n" +
               "$(TARGET): $(OBJECTS) | $(BUILDDIR)\n" +
               "\t$(CXX) $^ -o $@\n\n" +
               "$(BUILDDIR)/%.o: $(SRCDIR)/%.cpp | $(BUILDDIR)\n" +
               "\t$(CXX) $(CXXFLAGS) -I$(INCDIR) -c $< -o $@\n\n" +
               "$(BUILDDIR):\n" +
               "\tmkdir -p $(BUILDDIR)\n\n" +
               "clean:\n" +
               "\trm -rf $(BUILDDIR)\n\n" +
               ".PHONY: all clean\n";
    }
    
    private Map<String, String> generateGenericBuildConfig() {
        Map<String, String> config = new HashMap<>();
        
        config.put("Makefile", "# Generic Makefile\n" +
                              "all:\n" +
                              "\techo \"Build target for " + targetLanguage + "\"\n\n" +
                              "clean:\n" +
                              "\techo \"Clean target\"\n\n" +
                              ".PHONY: all clean\n");
        
        return config;
    }
    
    // Dependency mapping methods (simplified implementations)
    private String mapDependencyToJava(String dep) {
        // This would use a comprehensive mapping database in practice
        if (dep.contains("json")) {
            return "        <dependency>\n" +
                   "            <groupId>com.fasterxml.jackson.core</groupId>\n" +
                   "            <artifactId>jackson-databind</artifactId>\n" +
                   "            <version>2.15.2</version>\n" +
                   "        </dependency>";
        }
        return null;
    }
    
    private String mapDependencyToGradle(String dep) {
        if (dep.contains("json")) {
            return "implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.2'";
        }
        return null;
    }
    
    private String mapDependencyToPython(String dep) {
        if (dep.contains("http")) return "requests>=2.28.0";
        if (dep.contains("json")) return "json";  // Built-in
        return null;
    }
    
    private String mapDependencyToJavaScript(String dep) {
        if (dep.contains("http")) return "\"axios\": \"^1.3.0\"";
        return null;
    }
    
    private String mapDependencyToRust(String dep) {
        if (dep.contains("http")) return "reqwest = \"0.11\"";
        if (dep.contains("json")) return "serde_json = \"1.0\"";
        return null;
    }
}