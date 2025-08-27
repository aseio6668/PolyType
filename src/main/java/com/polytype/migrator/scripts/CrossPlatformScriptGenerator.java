package com.polytype.migrator.scripts;

import com.polytype.migrator.core.TargetLanguage;
import java.util.*;

/**
 * Generates cross-platform scripts (.sh and .bat) for different target languages.
 * Ensures compatibility across Unix/Linux, macOS, and Windows platforms.
 */
public class CrossPlatformScriptGenerator {
    
    private final ProjectAnalysisResult projectAnalysis;
    private final TargetLanguage targetLanguage;
    
    public CrossPlatformScriptGenerator(ProjectAnalysisResult projectAnalysis, TargetLanguage targetLanguage) {
        this.projectAnalysis = projectAnalysis;
        this.targetLanguage = targetLanguage;
    }
    
    /**
     * Generate all cross-platform scripts for the project.
     */
    public Map<String, String> generateAllScripts() {
        Map<String, String> scripts = new HashMap<>();
        
        // Build scripts
        scripts.putAll(generateBuildScripts());
        
        // Setup/Installation scripts
        scripts.putAll(generateSetupScripts());
        
        // Runtime scripts
        scripts.putAll(generateRuntimeScripts());
        
        // Testing scripts
        scripts.putAll(generateTestScripts());
        
        // Deployment scripts
        scripts.putAll(generateDeploymentScripts());
        
        // Development environment scripts
        scripts.putAll(generateDevScripts());
        
        return scripts;
    }
    
    /**
     * Generate build scripts for both Unix and Windows.
     */
    public Map<String, String> generateBuildScripts() {
        Map<String, String> scripts = new HashMap<>();
        
        scripts.put("build.sh", generateUnixBuildScript());
        scripts.put("build.bat", generateWindowsBuildScript());
        scripts.put("clean.sh", generateUnixCleanScript());
        scripts.put("clean.bat", generateWindowsCleanScript());
        
        return scripts;
    }
    
    /**
     * Generate setup/installation scripts.
     */
    public Map<String, String> generateSetupScripts() {
        Map<String, String> scripts = new HashMap<>();
        
        scripts.put("setup.sh", generateUnixSetupScript());
        scripts.put("setup.bat", generateWindowsSetupScript());
        scripts.put("install-dependencies.sh", generateUnixDependencyInstallScript());
        scripts.put("install-dependencies.bat", generateWindowsDependencyInstallScript());
        
        return scripts;
    }
    
    /**
     * Generate runtime scripts.
     */
    public Map<String, String> generateRuntimeScripts() {
        Map<String, String> scripts = new HashMap<>();
        
        scripts.put("run.sh", generateUnixRunScript());
        scripts.put("run.bat", generateWindowsRunScript());
        scripts.put("start.sh", generateUnixStartScript());
        scripts.put("start.bat", generateWindowsStartScript());
        scripts.put("stop.sh", generateUnixStopScript());
        scripts.put("stop.bat", generateWindowsStopScript());
        
        return scripts;
    }
    
    /**
     * Generate testing scripts.
     */
    public Map<String, String> generateTestScripts() {
        Map<String, String> scripts = new HashMap<>();
        
        scripts.put("test.sh", generateUnixTestScript());
        scripts.put("test.bat", generateWindowsTestScript());
        scripts.put("test-coverage.sh", generateUnixCoverageScript());
        scripts.put("test-coverage.bat", generateWindowsCoverageScript());
        
        return scripts;
    }
    
    /**
     * Generate deployment scripts.
     */
    public Map<String, String> generateDeploymentScripts() {
        Map<String, String> scripts = new HashMap<>();
        
        scripts.put("deploy.sh", generateUnixDeployScript());
        scripts.put("deploy.bat", generateWindowsDeployScript());
        scripts.put("package.sh", generateUnixPackageScript());
        scripts.put("package.bat", generateWindowsPackageScript());
        
        return scripts;
    }
    
    /**
     * Generate development environment scripts.
     */
    public Map<String, String> generateDevScripts() {
        Map<String, String> scripts = new HashMap<>();
        
        scripts.put("dev-setup.sh", generateUnixDevSetupScript());
        scripts.put("dev-setup.bat", generateWindowsDevSetupScript());
        scripts.put("format.sh", generateUnixFormatScript());
        scripts.put("format.bat", generateWindowsFormatScript());
        scripts.put("lint.sh", generateUnixLintScript());
        scripts.put("lint.bat", generateWindowsLintScript());
        
        return scripts;
    }
    
    // Unix/Linux/macOS Script Generators
    
    private String generateUnixBuildScript() {
        StringBuilder script = new StringBuilder();
        script.append(getUnixScriptHeader("Build Script", "Builds the " + targetLanguage + " project"));
        
        script.append("# Set error handling\n");
        script.append("set -e\n");
        script.append("set -u\n\n");
        
        script.append("# Colors for output\n");
        script.append("RED='\\033[0;31m'\n");
        script.append("GREEN='\\033[0;32m'\n");
        script.append("YELLOW='\\033[1;33m'\n");
        script.append("NC='\\033[0m' # No Color\n\n");
        
        script.append("echo \"${GREEN}Starting build process...${NC}\"\n\n");
        
        // Language-specific build commands
        switch (targetLanguage) {
            case JAVA:
                script.append(generateJavaUnixBuildCommands());
                break;
            case CPP:
                script.append(generateCppUnixBuildCommands());
                break;
            case PYTHON:
                script.append(generatePythonUnixBuildCommands());
                break;
            case JAVASCRIPT:
                script.append(generateJavaScriptUnixBuildCommands());
                break;
            case RUST:
                script.append(generateRustUnixBuildCommands());
                break;
            case GO:
                script.append(generateGoUnixBuildCommands());
                break;
            default:
                script.append("echo \"${YELLOW}Generic build process${NC}\"\n");
                script.append("# Add your build commands here\n");
        }
        
        script.append("\necho \"${GREEN}Build completed successfully!${NC}\"\n");
        return script.toString();
    }
    
    private String generateWindowsBuildScript() {
        StringBuilder script = new StringBuilder();
        script.append(getWindowsScriptHeader("Build Script", "Builds the " + targetLanguage + " project"));
        
        script.append("setlocal EnableDelayedExpansion\n");
        script.append("set \"ERROR_OCCURRED=false\"\n\n");
        
        script.append("echo Starting build process...\n\n");
        
        // Language-specific build commands
        switch (targetLanguage) {
            case JAVA:
                script.append(generateJavaWindowsBuildCommands());
                break;
            case CPP:
                script.append(generateCppWindowsBuildCommands());
                break;
            case PYTHON:
                script.append(generatePythonWindowsBuildCommands());
                break;
            case JAVASCRIPT:
                script.append(generateJavaScriptWindowsBuildCommands());
                break;
            case RUST:
                script.append(generateRustWindowsBuildCommands());
                break;
            case GO:
                script.append(generateGoWindowsBuildCommands());
                break;
            default:
                script.append("echo Generic build process\n");
                script.append("REM Add your build commands here\n");
        }
        
        script.append("\nif \"!ERROR_OCCURRED!\"==\"false\" (\n");
        script.append("    echo Build completed successfully!\n");
        script.append("    exit /b 0\n");
        script.append(") else (\n");
        script.append("    echo Build failed!\n");
        script.append("    exit /b 1\n");
        script.append(")\n");
        
        return script.toString();
    }
    
    // Language-specific Unix build commands
    
    private String generateJavaUnixBuildCommands() {
        StringBuilder commands = new StringBuilder();
        
        commands.append("# Detect Java build system\n");
        commands.append("if [ -f \"pom.xml\" ]; then\n");
        commands.append("    echo \"${YELLOW}Using Maven${NC}\"\n");
        commands.append("    if [ -f \"./mvnw\" ]; then\n");
        commands.append("        echo \"Using Maven wrapper\"\n");
        commands.append("        ./mvnw clean compile package -DskipTests\n");
        commands.append("    else\n");
        commands.append("        echo \"Using system Maven\"\n");
        commands.append("        mvn clean compile package -DskipTests\n");
        commands.append("    fi\n");
        commands.append("elif [ -f \"build.gradle\" ]; then\n");
        commands.append("    echo \"${YELLOW}Using Gradle${NC}\"\n");
        commands.append("    if [ -f \"./gradlew\" ]; then\n");
        commands.append("        echo \"Using Gradle wrapper\"\n");
        commands.append("        ./gradlew clean build -x test\n");
        commands.append("    else\n");
        commands.append("        echo \"Using system Gradle\"\n");
        commands.append("        gradle clean build -x test\n");
        commands.append("    fi\n");
        commands.append("else\n");
        commands.append("    echo \"${RED}No build system found (pom.xml or build.gradle)${NC}\"\n");
        commands.append("    echo \"Attempting manual compilation...\"\n");
        commands.append("    find src -name \"*.java\" | xargs javac -cp \"lib/*\" -d build/\n");
        commands.append("fi\n");
        
        return commands.toString();
    }
    
    private String generateCppUnixBuildCommands() {
        StringBuilder commands = new StringBuilder();
        
        commands.append("# Detect C++ build system\n");
        commands.append("if [ -f \"CMakeLists.txt\" ]; then\n");
        commands.append("    echo \"${YELLOW}Using CMake${NC}\"\n");
        commands.append("    mkdir -p build\n");
        commands.append("    cd build\n");
        commands.append("    cmake ..\n");
        commands.append("    make -j$(nproc)\n");
        commands.append("    cd ..\n");
        commands.append("elif [ -f \"Makefile\" ]; then\n");
        commands.append("    echo \"${YELLOW}Using Make${NC}\"\n");
        commands.append("    make clean\n");
        commands.append("    make -j$(nproc)\n");
        commands.append("else\n");
        commands.append("    echo \"${RED}No build system found (CMakeLists.txt or Makefile)${NC}\"\n");
        commands.append("    echo \"Attempting manual compilation...\"\n");
        commands.append("    g++ -std=c++17 -O2 src/*.cpp -o build/main\n");
        commands.append("fi\n");
        
        return commands.toString();
    }
    
    private String generatePythonUnixBuildCommands() {
        StringBuilder commands = new StringBuilder();
        
        commands.append("# Python build process\n");
        commands.append("echo \"${YELLOW}Setting up Python environment${NC}\"\n");
        commands.append("if [ -f \"requirements.txt\" ]; then\n");
        commands.append("    echo \"Installing dependencies...\"\n");
        commands.append("    python3 -m pip install -r requirements.txt\n");
        commands.append("fi\n");
        commands.append("if [ -f \"setup.py\" ]; then\n");
        commands.append("    echo \"Building Python package...\"\n");
        commands.append("    python3 setup.py build\n");
        commands.append("elif [ -f \"pyproject.toml\" ]; then\n");
        commands.append("    echo \"Building with modern Python tools...\"\n");
        commands.append("    pip install build\n");
        commands.append("    python3 -m build\n");
        commands.append("fi\n");
        commands.append("echo \"Compiling Python files...\"\n");
        commands.append("python3 -m compileall . || true\n");
        
        return commands.toString();
    }
    
    private String generateJavaScriptUnixBuildCommands() {
        StringBuilder commands = new StringBuilder();
        
        commands.append("# JavaScript/Node.js build process\n");
        commands.append("if [ -f \"package.json\" ]; then\n");
        commands.append("    echo \"${YELLOW}Installing dependencies${NC}\"\n");
        commands.append("    if command -v yarn >/dev/null 2>&1; then\n");
        commands.append("        echo \"Using Yarn\"\n");
        commands.append("        yarn install\n");
        commands.append("        if yarn run | grep -q \"build\"; then\n");
        commands.append("            echo \"Running build script\"\n");
        commands.append("            yarn build\n");
        commands.append("        fi\n");
        commands.append("    else\n");
        commands.append("        echo \"Using npm\"\n");
        commands.append("        npm install\n");
        commands.append("        if npm run | grep -q \"build\"; then\n");
        commands.append("            echo \"Running build script\"\n");
        commands.append("            npm run build\n");
        commands.append("        fi\n");
        commands.append("    fi\n");
        commands.append("else\n");
        commands.append("    echo \"${RED}No package.json found${NC}\"\n");
        commands.append("fi\n");
        
        return commands.toString();
    }
    
    private String generateRustUnixBuildCommands() {
        StringBuilder commands = new StringBuilder();
        
        commands.append("# Rust build process\n");
        commands.append("if [ -f \"Cargo.toml\" ]; then\n");
        commands.append("    echo \"${YELLOW}Building Rust project${NC}\"\n");
        commands.append("    cargo build --release\n");
        commands.append("else\n");
        commands.append("    echo \"${RED}No Cargo.toml found${NC}\"\n");
        commands.append("    exit 1\n");
        commands.append("fi\n");
        
        return commands.toString();
    }
    
    private String generateGoUnixBuildCommands() {
        StringBuilder commands = new StringBuilder();
        
        commands.append("# Go build process\n");
        commands.append("if [ -f \"go.mod\" ]; then\n");
        commands.append("    echo \"${YELLOW}Building Go project${NC}\"\n");
        commands.append("    go mod tidy\n");
        commands.append("    go build -o build/main .\n");
        commands.append("else\n");
        commands.append("    echo \"${YELLOW}Building Go project (legacy GOPATH mode)${NC}\"\n");
        commands.append("    go build -o build/main *.go\n");
        commands.append("fi\n");
        
        return commands.toString();
    }
    
    // Language-specific Windows build commands
    
    private String generateJavaWindowsBuildCommands() {
        StringBuilder commands = new StringBuilder();
        
        commands.append("REM Detect Java build system\n");
        commands.append("if exist \"pom.xml\" (\n");
        commands.append("    echo Using Maven\n");
        commands.append("    if exist \"mvnw.cmd\" (\n");
        commands.append("        echo Using Maven wrapper\n");
        commands.append("        mvnw.cmd clean compile package -DskipTests\n");
        commands.append("        if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append("    ) else (\n");
        commands.append("        echo Using system Maven\n");
        commands.append("        mvn clean compile package -DskipTests\n");
        commands.append("        if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append("    )\n");
        commands.append(") else if exist \"build.gradle\" (\n");
        commands.append("    echo Using Gradle\n");
        commands.append("    if exist \"gradlew.bat\" (\n");
        commands.append("        echo Using Gradle wrapper\n");
        commands.append("        gradlew.bat clean build -x test\n");
        commands.append("        if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append("    ) else (\n");
        commands.append("        echo Using system Gradle\n");
        commands.append("        gradle clean build -x test\n");
        commands.append("        if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append("    )\n");
        commands.append(") else (\n");
        commands.append("    echo No build system found (pom.xml or build.gradle)\n");
        commands.append("    echo Attempting manual compilation...\n");
        commands.append("    mkdir build 2>nul\n");
        commands.append("    dir /s /b src\\*.java > sources.txt\n");
        commands.append("    javac -cp \"lib\\*\" -d build @sources.txt\n");
        commands.append("    if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append("    del sources.txt\n");
        commands.append(")\n");
        
        return commands.toString();
    }
    
    private String generateCppWindowsBuildCommands() {
        StringBuilder commands = new StringBuilder();
        
        commands.append("REM Detect C++ build system\n");
        commands.append("if exist \"CMakeLists.txt\" (\n");
        commands.append("    echo Using CMake\n");
        commands.append("    mkdir build 2>nul\n");
        commands.append("    cd build\n");
        commands.append("    cmake ..\n");
        commands.append("    if errorlevel 1 (\n");
        commands.append("        set \"ERROR_OCCURRED=true\"\n");
        commands.append("        cd ..\n");
        commands.append("        goto :eof\n");
        commands.append("    )\n");
        commands.append("    cmake --build . --config Release\n");
        commands.append("    if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append("    cd ..\n");
        commands.append(") else if exist \"Makefile\" (\n");
        commands.append("    echo Using Make\n");
        commands.append("    make clean\n");
        commands.append("    make\n");
        commands.append("    if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append(") else (\n");
        commands.append("    echo No build system found (CMakeLists.txt or Makefile)\n");
        commands.append("    echo Attempting manual compilation with MSVC...\n");
        commands.append("    mkdir build 2>nul\n");
        commands.append("    cl /EHsc /std:c++17 src\\*.cpp /Fe:build\\main.exe\n");
        commands.append("    if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append(")\n");
        
        return commands.toString();
    }
    
    private String generatePythonWindowsBuildCommands() {
        StringBuilder commands = new StringBuilder();
        
        commands.append("REM Python build process\n");
        commands.append("echo Setting up Python environment\n");
        commands.append("if exist \"requirements.txt\" (\n");
        commands.append("    echo Installing dependencies...\n");
        commands.append("    python -m pip install -r requirements.txt\n");
        commands.append("    if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append(")\n");
        commands.append("if exist \"setup.py\" (\n");
        commands.append("    echo Building Python package...\n");
        commands.append("    python setup.py build\n");
        commands.append("    if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append(") else if exist \"pyproject.toml\" (\n");
        commands.append("    echo Building with modern Python tools...\n");
        commands.append("    pip install build\n");
        commands.append("    python -m build\n");
        commands.append("    if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append(")\n");
        commands.append("echo Compiling Python files...\n");
        commands.append("python -m compileall . 2>nul\n");
        
        return commands.toString();
    }
    
    private String generateJavaScriptWindowsBuildCommands() {
        StringBuilder commands = new StringBuilder();
        
        commands.append("REM JavaScript/Node.js build process\n");
        commands.append("if exist \"package.json\" (\n");
        commands.append("    echo Installing dependencies\n");
        commands.append("    where yarn >nul 2>nul\n");
        commands.append("    if %errorlevel% == 0 (\n");
        commands.append("        echo Using Yarn\n");
        commands.append("        yarn install\n");
        commands.append("        if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append("        yarn run | findstr \"build\" >nul\n");
        commands.append("        if %errorlevel% == 0 (\n");
        commands.append("            echo Running build script\n");
        commands.append("            yarn build\n");
        commands.append("            if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append("        )\n");
        commands.append("    ) else (\n");
        commands.append("        echo Using npm\n");
        commands.append("        npm install\n");
        commands.append("        if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append("        npm run | findstr \"build\" >nul\n");
        commands.append("        if %errorlevel% == 0 (\n");
        commands.append("            echo Running build script\n");
        commands.append("            npm run build\n");
        commands.append("            if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append("        )\n");
        commands.append("    )\n");
        commands.append(") else (\n");
        commands.append("    echo No package.json found\n");
        commands.append("    set \"ERROR_OCCURRED=true\"\n");
        commands.append(")\n");
        
        return commands.toString();
    }
    
    private String generateRustWindowsBuildCommands() {
        StringBuilder commands = new StringBuilder();
        
        commands.append("REM Rust build process\n");
        commands.append("if exist \"Cargo.toml\" (\n");
        commands.append("    echo Building Rust project\n");
        commands.append("    cargo build --release\n");
        commands.append("    if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append(") else (\n");
        commands.append("    echo No Cargo.toml found\n");
        commands.append("    set \"ERROR_OCCURRED=true\"\n");
        commands.append(")\n");
        
        return commands.toString();
    }
    
    private String generateGoWindowsBuildCommands() {
        StringBuilder commands = new StringBuilder();
        
        commands.append("REM Go build process\n");
        commands.append("if exist \"go.mod\" (\n");
        commands.append("    echo Building Go project\n");
        commands.append("    go mod tidy\n");
        commands.append("    if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append("    mkdir build 2>nul\n");
        commands.append("    go build -o build\\main.exe .\n");
        commands.append("    if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append(") else (\n");
        commands.append("    echo Building Go project (legacy GOPATH mode)\n");
        commands.append("    mkdir build 2>nul\n");
        commands.append("    go build -o build\\main.exe *.go\n");
        commands.append("    if errorlevel 1 set \"ERROR_OCCURRED=true\"\n");
        commands.append(")\n");
        
        return commands.toString();
    }
    
    // Utility methods for script generation
    
    private String getUnixScriptHeader(String title, String description) {
        StringBuilder header = new StringBuilder();
        header.append("#!/bin/bash\n");
        header.append("#\n");
        header.append("# ").append(title).append(" - Generated by PolyType\n");
        header.append("# ").append(description).append("\n");
        header.append("#\n");
        header.append("# This script provides cross-platform compatibility for Unix/Linux/macOS\n");
        header.append("#\n\n");
        return header.toString();
    }
    
    private String getWindowsScriptHeader(String title, String description) {
        StringBuilder header = new StringBuilder();
        header.append("@echo off\n");
        header.append("REM\n");
        header.append("REM ").append(title).append(" - Generated by PolyType\n");
        header.append("REM ").append(description).append("\n");
        header.append("REM\n");
        header.append("REM This script provides Windows compatibility\n");
        header.append("REM\n\n");
        return header.toString();
    }
    
    // Placeholder methods for other script types (implement as needed)
    private String generateUnixCleanScript() {
        return getUnixScriptHeader("Clean Script", "Cleans build artifacts") +
               "echo \"Cleaning build artifacts...\"\n" +
               "rm -rf build/ target/ dist/ node_modules/.cache/\n" +
               "echo \"Clean completed!\"\n";
    }
    
    private String generateWindowsCleanScript() {
        return getWindowsScriptHeader("Clean Script", "Cleans build artifacts") +
               "echo Cleaning build artifacts...\n" +
               "if exist build\\ rmdir /s /q build\n" +
               "if exist target\\ rmdir /s /q target\n" +
               "if exist dist\\ rmdir /s /q dist\n" +
               "echo Clean completed!\n";
    }
    
    private String generateUnixSetupScript() { return getUnixScriptHeader("Setup Script", "Sets up development environment"); }
    private String generateWindowsSetupScript() { return getWindowsScriptHeader("Setup Script", "Sets up development environment"); }
    private String generateUnixDependencyInstallScript() { return getUnixScriptHeader("Dependency Install", "Installs project dependencies"); }
    private String generateWindowsDependencyInstallScript() { return getWindowsScriptHeader("Dependency Install", "Installs project dependencies"); }
    private String generateUnixRunScript() { return getUnixScriptHeader("Run Script", "Runs the application"); }
    private String generateWindowsRunScript() { return getWindowsScriptHeader("Run Script", "Runs the application"); }
    private String generateUnixStartScript() { return getUnixScriptHeader("Start Script", "Starts the application as service"); }
    private String generateWindowsStartScript() { return getWindowsScriptHeader("Start Script", "Starts the application as service"); }
    private String generateUnixStopScript() { return getUnixScriptHeader("Stop Script", "Stops the application service"); }
    private String generateWindowsStopScript() { return getWindowsScriptHeader("Stop Script", "Stops the application service"); }
    private String generateUnixTestScript() { return getUnixScriptHeader("Test Script", "Runs tests"); }
    private String generateWindowsTestScript() { return getWindowsScriptHeader("Test Script", "Runs tests"); }
    private String generateUnixCoverageScript() { return getUnixScriptHeader("Coverage Script", "Runs tests with coverage"); }
    private String generateWindowsCoverageScript() { return getWindowsScriptHeader("Coverage Script", "Runs tests with coverage"); }
    private String generateUnixDeployScript() { return getUnixScriptHeader("Deploy Script", "Deploys the application"); }
    private String generateWindowsDeployScript() { return getWindowsScriptHeader("Deploy Script", "Deploys the application"); }
    private String generateUnixPackageScript() { return getUnixScriptHeader("Package Script", "Packages the application"); }
    private String generateWindowsPackageScript() { return getWindowsScriptHeader("Package Script", "Packages the application"); }
    private String generateUnixDevSetupScript() { return getUnixScriptHeader("Dev Setup", "Sets up development environment"); }
    private String generateWindowsDevSetupScript() { return getWindowsScriptHeader("Dev Setup", "Sets up development environment"); }
    private String generateUnixFormatScript() { return getUnixScriptHeader("Format Script", "Formats source code"); }
    private String generateWindowsFormatScript() { return getWindowsScriptHeader("Format Script", "Formats source code"); }
    private String generateUnixLintScript() { return getUnixScriptHeader("Lint Script", "Lints source code"); }
    private String generateWindowsLintScript() { return getWindowsScriptHeader("Lint Script", "Lints source code"); }
}