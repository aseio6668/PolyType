package com.polytype.migrator.scripts.parsers;

import com.polytype.migrator.scripts.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for CMakeLists.txt files.
 * Extracts project information, targets, dependencies, and configuration.
 */
public class CMakeListsParser implements ScriptParser {
    
    private static final Pattern CMAKE_MINIMUM_REQUIRED_PATTERN = Pattern.compile(
        "cmake_minimum_required\\s*\\(\\s*VERSION\\s+([0-9.]+)\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PROJECT_PATTERN = Pattern.compile(
        "project\\s*\\(\\s*([^\\s)]+)(?:\\s+([^)]+))?\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ADD_EXECUTABLE_PATTERN = Pattern.compile(
        "add_executable\\s*\\(\\s*([^\\s)]+)\\s+([^)]+)\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ADD_LIBRARY_PATTERN = Pattern.compile(
        "add_library\\s*\\(\\s*([^\\s)]+)(?:\\s+(STATIC|SHARED|MODULE))?\\s+([^)]+)\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern FIND_PACKAGE_PATTERN = Pattern.compile(
        "find_package\\s*\\(\\s*([^\\s)]+)(?:\\s+([^)]+))?\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TARGET_LINK_LIBRARIES_PATTERN = Pattern.compile(
        "target_link_libraries\\s*\\(\\s*([^\\s)]+)\\s+([^)]+)\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SET_PATTERN = Pattern.compile(
        "set\\s*\\(\\s*([^\\s)]+)\\s+([^)]+)\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern INCLUDE_DIRECTORIES_PATTERN = Pattern.compile(
        "include_directories\\s*\\(\\s*([^)]+)\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public ScriptAnalysisResult parseScript(Path scriptFile) throws IOException {
        ScriptAnalysisResult result = new ScriptAnalysisResult();
        result.setScriptType(ScriptAnalyzer.ScriptType.CMAKE);
        result.setFilePath(scriptFile.toString());
        result.setLanguage("CMake");
        result.setPurpose(ScriptAnalysisResult.ScriptPurpose.BUILD);
        
        List<String> lines = Files.readAllLines(scriptFile);
        String content = String.join("\n", lines);
        
        // Parse CMake minimum version
        Matcher cmakeVersionMatcher = CMAKE_MINIMUM_REQUIRED_PATTERN.matcher(content);
        if (cmakeVersionMatcher.find()) {
            String version = cmakeVersionMatcher.group(1);
            result.setVersion(version);
            result.addConfiguration("cmake_minimum_version", version);
        }
        
        // Parse project information
        Matcher projectMatcher = PROJECT_PATTERN.matcher(content);
        if (projectMatcher.find()) {
            String projectName = projectMatcher.group(1);
            result.addConfiguration("project_name", projectName);
            result.addMetadata("name", projectName);
            
            String projectDetails = projectMatcher.group(2);
            if (projectDetails != null) {
                parseProjectDetails(projectDetails, result);
            }
        }
        
        // Parse executables
        Matcher executableMatcher = ADD_EXECUTABLE_PATTERN.matcher(content);
        while (executableMatcher.find()) {
            String targetName = executableMatcher.group(1);
            String sources = executableMatcher.group(2);
            
            result.addBuildTarget(targetName);
            result.addMetadata("target_type_" + targetName, "executable");
            
            // Parse source files to detect language
            parseSourceFiles(sources, result);
        }
        
        // Parse libraries
        Matcher libraryMatcher = ADD_LIBRARY_PATTERN.matcher(content);
        while (libraryMatcher.find()) {
            String targetName = libraryMatcher.group(1);
            String libraryType = libraryMatcher.group(2);
            String sources = libraryMatcher.group(3);
            
            result.addBuildTarget(targetName);
            result.addMetadata("target_type_" + targetName, "library");
            
            if (libraryType != null) {
                result.addConfiguration("library_type_" + targetName, libraryType.toLowerCase());
            }
            
            parseSourceFiles(sources, result);
        }
        
        // Parse package dependencies
        Matcher packageMatcher = FIND_PACKAGE_PATTERN.matcher(content);
        while (packageMatcher.find()) {
            String packageName = packageMatcher.group(1);
            String packageDetails = packageMatcher.group(2);
            
            result.addDependency(packageName);
            
            if (packageDetails != null && packageDetails.contains("REQUIRED")) {
                result.addMetadata("required_package_" + packageName, "true");
            }
        }
        
        // Parse target link libraries
        Matcher linkLibrariesMatcher = TARGET_LINK_LIBRARIES_PATTERN.matcher(content);
        while (linkLibrariesMatcher.find()) {
            String targetName = linkLibrariesMatcher.group(1);
            String libraries = linkLibrariesMatcher.group(2);
            
            String[] libs = libraries.split("\\s+");
            for (String lib : libs) {
                lib = lib.trim();
                if (!lib.isEmpty() && !lib.equals("PUBLIC") && !lib.equals("PRIVATE") && !lib.equals("INTERFACE")) {
                    result.addDependency(lib);
                    result.addMetadata("link_library_" + targetName, lib);
                }
            }
        }
        
        // Parse variables
        Matcher setMatcher = SET_PATTERN.matcher(content);
        while (setMatcher.find()) {
            String varName = setMatcher.group(1);
            String varValue = setMatcher.group(2);
            
            result.addVariable(varName, varValue);
            
            // Detect C++ standard
            if ("CMAKE_CXX_STANDARD".equals(varName)) {
                result.addConfiguration("cpp_standard", varValue);
                result.addEmbeddedLanguage("C++");
            } else if ("CMAKE_C_STANDARD".equals(varName)) {
                result.addConfiguration("c_standard", varValue);
                result.addEmbeddedLanguage("C");
            }
        }
        
        // Parse include directories
        Matcher includeMatcher = INCLUDE_DIRECTORIES_PATTERN.matcher(content);
        while (includeMatcher.find()) {
            String directories = includeMatcher.group(1);
            String[] dirs = directories.split("\\s+");
            for (String dir : dirs) {
                dir = dir.trim();
                if (!dir.isEmpty()) {
                    result.addConfiguration("include_directory", dir);
                }
            }
        }
        
        // Detect primary language based on file extensions and configuration
        detectPrimaryLanguage(result);
        
        // Add CMake-specific configuration
        result.addConfiguration("build_system", "cmake");
        result.addConfiguration("generator", "Unix Makefiles"); // Default
        
        return result;
    }
    
    private void parseProjectDetails(String details, ScriptAnalysisResult result) {
        if (details.contains("VERSION")) {
            Pattern versionPattern = Pattern.compile("VERSION\\s+([0-9.]+)");
            Matcher matcher = versionPattern.matcher(details);
            if (matcher.find()) {
                result.addConfiguration("project_version", matcher.group(1));
            }
        }
        
        if (details.contains("LANGUAGES")) {
            Pattern languagePattern = Pattern.compile("LANGUAGES\\s+([^\\s)]+)");
            Matcher matcher = languagePattern.matcher(details);
            if (matcher.find()) {
                String languages = matcher.group(1);
                String[] langs = languages.split("\\s+");
                for (String lang : langs) {
                    result.addEmbeddedLanguage(lang);
                }
            }
        }
    }
    
    private void parseSourceFiles(String sources, ScriptAnalysisResult result) {
        String[] files = sources.split("\\s+");
        
        for (String file : files) {
            file = file.trim();
            if (file.isEmpty()) continue;
            
            // Detect language by file extension
            if (file.endsWith(".cpp") || file.endsWith(".cxx") || file.endsWith(".cc")) {
                result.addEmbeddedLanguage("C++");
            } else if (file.endsWith(".c")) {
                result.addEmbeddedLanguage("C");
            } else if (file.endsWith(".h") || file.endsWith(".hpp")) {
                // Header file - could be C or C++, check for C++ features
                result.addEmbeddedLanguage("C++"); // Assume C++ for headers in CMake
            }
        }
    }
    
    private void detectPrimaryLanguage(ScriptAnalysisResult result) {
        if (result.getEmbeddedLanguages().contains("C++")) {
            result.setLanguage("C++");
        } else if (result.getEmbeddedLanguages().contains("C")) {
            result.setLanguage("C");
        } else {
            // Default to C++ for CMake projects
            result.setLanguage("C++");
            result.addEmbeddedLanguage("C++");
        }
    }
    
    @Override
    public ScriptAnalyzer.ScriptType getSupportedType() {
        return ScriptAnalyzer.ScriptType.CMAKE;
    }
    
    @Override
    public boolean canHandle(Path scriptFile) {
        String filename = scriptFile.getFileName().toString();
        return "CMakeLists.txt".equals(filename) || filename.toLowerCase().endsWith(".cmake");
    }
}