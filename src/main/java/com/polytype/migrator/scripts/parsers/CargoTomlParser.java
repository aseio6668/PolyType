package com.polytype.migrator.scripts.parsers;

import com.polytype.migrator.scripts.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Cargo.toml files (Rust package manager configuration).
 * Analyzes Rust project configuration, dependencies, and build settings.
 */
public class CargoTomlParser implements ScriptParser {
    
    @Override
    public ScriptAnalyzer.ScriptType getSupportedType() {
        return ScriptAnalyzer.ScriptType.CARGO_TOML;
    }
    
    @Override
    public boolean canHandle(Path scriptFile) {
        String fileName = scriptFile.getFileName().toString().toLowerCase();
        return fileName.equals("cargo.toml");
    }
    
    // Pattern for TOML sections
    private static final Pattern SECTION_PATTERN = Pattern.compile("^\\[([^\\]]+)\\]\\s*$");
    
    // Pattern for key-value pairs
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^\\s*(\\w+(?:[\\w-]*\\w)?)\\s*=\\s*(.+)$");
    
    // Pattern for string values (quoted)
    private static final Pattern STRING_VALUE_PATTERN = Pattern.compile("^['\"](.+)['\"]$");
    
    // Pattern for dependency specifications
    private static final Pattern DEPENDENCY_SPEC_PATTERN = Pattern.compile(
        "^\\s*(\\w+(?:[\\w-]*\\w)?)\\s*=\\s*(?:['\"]([^'\"]+)['\"]|\\{(.+)\\})\\s*$"
    );

    @Override
    public ScriptAnalysisResult parseScript(Path scriptFile) throws IOException {
        ScriptAnalysisResult result = new ScriptAnalysisResult();
        result.setScriptType(ScriptAnalyzer.ScriptType.CARGO_TOML);
        result.setFilePath(scriptFile.toString());
        result.setLanguage("TOML");
        result.setPurpose(ScriptAnalysisResult.ScriptPurpose.BUILD);
        result.addEmbeddedLanguage("Rust");
        
        List<String> lines = Files.readAllLines(scriptFile);
        if (lines.isEmpty()) {
            return result;
        }
        
        String currentSection = null;
        Map<String, Map<String, String>> sections = new HashMap<>();
        Map<String, String> currentSectionContent = new HashMap<>();
        
        // Parse TOML structure
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Skip empty lines and comments
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }
            
            // Check for section headers
            Matcher sectionMatcher = SECTION_PATTERN.matcher(trimmedLine);
            if (sectionMatcher.find()) {
                // Save previous section
                if (currentSection != null && !currentSectionContent.isEmpty()) {
                    sections.put(currentSection, new HashMap<>(currentSectionContent));
                }
                
                currentSection = sectionMatcher.group(1);
                currentSectionContent.clear();
                continue;
            }
            
            // Parse key-value pairs
            Matcher kvMatcher = KEY_VALUE_PATTERN.matcher(trimmedLine);
            if (kvMatcher.find()) {
                String key = kvMatcher.group(1);
                String value = kvMatcher.group(2);
                
                // Clean quoted strings
                Matcher stringMatcher = STRING_VALUE_PATTERN.matcher(value);
                if (stringMatcher.find()) {
                    value = stringMatcher.group(1);
                }
                
                currentSectionContent.put(key, value);
                
                // Analyze based on current section
                if (currentSection != null) {
                    analyzeKeyValue(currentSection, key, value, result);
                }
            }
        }
        
        // Save last section
        if (currentSection != null && !currentSectionContent.isEmpty()) {
            sections.put(currentSection, currentSectionContent);
        }
        
        // Analyze sections
        analyzeSections(sections, result);
        
        return result;
    }
    
    private void analyzeKeyValue(String section, String key, String value, ScriptAnalysisResult result) {
        switch (section) {
            case "package":
                analyzePackageSection(key, value, result);
                break;
            case "dependencies":
                analyzeDependency(key, value, result, false);
                break;
            case "dev-dependencies":
                analyzeDependency(key, value, result, true);
                break;
            case "build-dependencies":
                analyzeBuildDependency(key, value, result);
                break;
            case "bin":
                analyzeBinaryTarget(key, value, result);
                break;
            case "lib":
                analyzeLibraryTarget(key, value, result);
                break;
            case "features":
                analyzeFeature(key, value, result);
                break;
            case "profile.release":
            case "profile.dev":
                analyzeProfile(section, key, value, result);
                break;
            default:
                if (section.startsWith("dependencies.")) {
                    String depName = section.substring("dependencies.".length());
                    analyzeDependency(depName, key + "=" + value, result, false);
                }
                break;
        }
    }
    
    private void analyzePackageSection(String key, String value, ScriptAnalysisResult result) {
        switch (key) {
            case "name":
                result.addMetadata("package_name", value);
                break;
            case "version":
                result.setVersion(value);
                break;
            case "authors":
                result.addMetadata("authors", value);
                break;
            case "edition":
                result.addMetadata("rust_edition", value);
                break;
            case "description":
                result.addMetadata("description", value);
                break;
            case "license":
                result.addMetadata("license", value);
                break;
            case "repository":
                result.addMetadata("repository", value);
                break;
            case "readme":
                result.addMetadata("readme", value);
                break;
            case "keywords":
                result.addMetadata("keywords", value);
                break;
            case "categories":
                result.addMetadata("categories", value);
                break;
        }
    }
    
    private void analyzeDependency(String name, String spec, ScriptAnalysisResult result, boolean isDev) {
        result.addDependency((isDev ? "dev:" : "") + name);
        
        // Analyze well-known Rust crates
        analyzeRustCrate(name, result);
        
        // Parse dependency specification for more details
        if (spec.contains("git =") || spec.contains("path =")) {
            result.addMetadata("dependency_" + name + "_source", 
                spec.contains("git =") ? "git" : "local");
        }
        
        if (spec.contains("features =")) {
            result.addMetadata("dependency_" + name + "_features", "enabled");
        }
        
        if (spec.contains("optional = true")) {
            result.addMetadata("dependency_" + name + "_optional", "true");
        }
    }
    
    private void analyzeRustCrate(String crateName, ScriptAnalysisResult result) {
        switch (crateName) {
            // Serialization
            case "serde":
            case "serde_json":
            case "serde_yaml":
            case "bincode":
            case "toml":
                result.addMetadata("capability_serialization", "true");
                break;
                
            // Web frameworks
            case "actix-web":
            case "warp":
            case "rocket":
            case "hyper":
            case "reqwest":
                result.addMetadata("capability_web", "true");
                result.addDependency("Web Framework");
                break;
                
            // Database
            case "diesel":
            case "sqlx":
            case "rusqlite":
            case "mongodb":
            case "redis":
                result.addMetadata("capability_database", "true");
                result.addDependency("Database");
                break;
                
            // Async runtime
            case "tokio":
            case "async-std":
            case "smol":
                result.addMetadata("capability_async", "true");
                result.addDependency("Async Runtime");
                break;
                
            // CLI
            case "clap":
            case "structopt":
            case "argh":
                result.addMetadata("capability_cli", "true");
                result.addDependency("CLI Framework");
                break;
                
            // Crypto
            case "ring":
            case "rustls":
            case "openssl":
            case "sha2":
            case "rand":
                result.addMetadata("capability_crypto", "true");
                result.addDependency("Cryptography");
                break;
                
            // Testing
            case "criterion":
            case "proptest":
            case "quickcheck":
                result.addDependency("Testing Framework");
                break;
                
            // Graphics/GUI
            case "wgpu":
            case "winit":
            case "pixels":
            case "iced":
            case "egui":
                result.addMetadata("capability_graphics", "true");
                result.addDependency("Graphics/GUI");
                break;
                
            // System programming
            case "libc":
            case "winapi":
            case "nix":
                result.addMetadata("capability_system", "true");
                break;
        }
    }
    
    private void analyzeBuildDependency(String name, String spec, ScriptAnalysisResult result) {
        result.addDependency("build:" + name);
        result.addMetadata("build_dependency", name);
        
        // Common build dependencies
        switch (name) {
            case "cc":
                result.addEmbeddedLanguage("C");
                result.addDependency("C Compiler");
                break;
            case "bindgen":
                result.addEmbeddedLanguage("C");
                result.addMetadata("capability_ffi", "true");
                break;
            case "prost-build":
            case "tonic-build":
                result.addDependency("Protocol Buffers");
                break;
        }
    }
    
    private void analyzeBinaryTarget(String key, String value, ScriptAnalysisResult result) {
        if (key.equals("name")) {
            result.addBuildTarget("bin:" + value);
            result.addMetadata("binary_" + value, "defined");
        }
    }
    
    private void analyzeLibraryTarget(String key, String value, ScriptAnalysisResult result) {
        if (key.equals("name")) {
            result.addBuildTarget("lib:" + value);
            result.addMetadata("library_name", value);
        } else if (key.equals("crate-type")) {
            result.addMetadata("library_type", value);
            
            // Analyze crate type
            if (value.contains("cdylib") || value.contains("dylib")) {
                result.addMetadata("capability_ffi", "true");
            } else if (value.contains("staticlib")) {
                result.addMetadata("static_library", "true");
            }
        }
    }
    
    private void analyzeFeature(String key, String value, ScriptAnalysisResult result) {
        result.addMetadata("feature_" + key, value);
        
        // Analyze common feature patterns
        if (key.equals("default")) {
            result.addMetadata("default_features", value);
        }
    }
    
    private void analyzeProfile(String section, String key, String value, ScriptAnalysisResult result) {
        String profileType = section.split("\\.")[1]; // "release" or "dev"
        result.addMetadata("profile_" + profileType + "_" + key, value);
        
        if (key.equals("opt-level")) {
            result.addMetadata("optimization_" + profileType, value);
        } else if (key.equals("lto")) {
            result.addMetadata("link_time_optimization", value);
        }
    }
    
    private void analyzeSections(Map<String, Map<String, String>> sections, ScriptAnalysisResult result) {
        // Determine project type based on targets
        boolean hasLib = sections.containsKey("lib");
        boolean hasBin = sections.containsKey("bin") || 
                        sections.keySet().stream().anyMatch(s -> s.startsWith("bin"));
        
        if (hasLib && hasBin) {
            result.addMetadata("project_type", "mixed");
        } else if (hasLib) {
            result.addMetadata("project_type", "library");
        } else if (hasBin) {
            result.addMetadata("project_type", "binary");
        } else {
            result.addMetadata("project_type", "unknown");
        }
        
        // Check for workspace configuration
        if (sections.containsKey("workspace")) {
            result.addMetadata("workspace", "true");
            Map<String, String> workspace = sections.get("workspace");
            if (workspace.containsKey("members")) {
                result.addMetadata("workspace_members", workspace.get("members"));
            }
        }
        
        // Analyze target-specific dependencies
        for (String section : sections.keySet()) {
            if (section.startsWith("target.")) {
                String target = section.substring(7); // Remove "target."
                if (target.contains(".dependencies")) {
                    String targetTriple = target.substring(0, target.indexOf(".dependencies"));
                    result.addMetadata("target_" + targetTriple.replace('-', '_'), "supported");
                }
            }
        }
        
        // Check for build scripts
        Map<String, String> packageInfo = sections.get("package");
        if (packageInfo != null && packageInfo.containsKey("build")) {
            result.addMetadata("build_script", packageInfo.get("build"));
            result.addBuildTarget("build-script");
        }
    }
}