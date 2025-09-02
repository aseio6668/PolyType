@echo off
REM ============================================================================
REM PolyType Code Migrator - Visual Studio 2022 Build Script
REM Enterprise-grade multi-language code migration platform
REM ============================================================================

setlocal enabledelayedexpansion

echo.
echo ================================================================================
echo POLYTYPE CODE MIGRATOR - VISUAL STUDIO 2022 BUILD SYSTEM
echo ================================================================================
echo Revolutionary multi-language migration with 180+ translation paths
echo Binary analysis, ML integration, legacy web modernization
echo ================================================================================
echo.

REM ============================================================================
REM Environment Detection and Setup
REM ============================================================================

echo [1/8] Detecting Visual Studio 2022 Environment...

REM Check for VS 2022 installation
set "VS2022_INSTALLED=false"
set "VSWHERE_PATH=%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe"

if exist "%VSWHERE_PATH%" (
    for /f "usebackq tokens=*" %%i in (`"%VSWHERE_PATH%" -version "[17.0,18.0)" -property installationPath`) do (
        set "VS2022_PATH=%%i"
        set "VS2022_INSTALLED=true"
        echo    ✓ Visual Studio 2022 found: %%i
    )
) else (
    echo    ⚠ vswhere.exe not found, checking common paths...
)

REM Fallback to common installation paths
if "%VS2022_INSTALLED%"=="false" (
    set "COMMON_PATHS[0]=%ProgramFiles%\Microsoft Visual Studio\2022\Enterprise"
    set "COMMON_PATHS[1]=%ProgramFiles%\Microsoft Visual Studio\2022\Professional"
    set "COMMON_PATHS[2]=%ProgramFiles%\Microsoft Visual Studio\2022\Community"
    set "COMMON_PATHS[3]=%ProgramFiles%\Microsoft Visual Studio\2022\BuildTools"
    set "COMMON_PATHS[4]=%ProgramFiles(x86)%\Microsoft Visual Studio\2022\Enterprise"
    set "COMMON_PATHS[5]=%ProgramFiles(x86)%\Microsoft Visual Studio\2022\Professional"
    set "COMMON_PATHS[6]=%ProgramFiles(x86)%\Microsoft Visual Studio\2022\Community"
    set "COMMON_PATHS[7]=%ProgramFiles(x86)%\Microsoft Visual Studio\2022\BuildTools"
    
    for /l %%i in (0,1,7) do (
        if exist "!COMMON_PATHS[%%i]!\Common7\Tools\VsDevCmd.bat" (
            set "VS2022_PATH=!COMMON_PATHS[%%i]!"
            set "VS2022_INSTALLED=true"
            echo    ✓ Visual Studio 2022 found: !COMMON_PATHS[%%i]!
            goto :vs_found
        )
    )
    :vs_found
)

if "%VS2022_INSTALLED%"=="false" (
    echo    ❌ Visual Studio 2022 not found!
    echo    Please install Visual Studio 2022 Community, Professional, or Enterprise
    echo    Download from: https://visualstudio.microsoft.com/vs/
    echo.
    echo    Required workloads:
    echo    - .NET desktop development
    echo    - Desktop development with C++
    echo    - Java development (if using Java components)
    pause
    exit /b 1
)

REM ============================================================================
REM Java Development Kit Detection
REM ============================================================================

echo [2/8] Detecting Java Development Kit...

set "JAVA_FOUND=false"

REM Check JAVA_HOME first
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        echo    ✓ Java JDK found via JAVA_HOME: %JAVA_HOME%
        set "JAVA_FOUND=true"
        set "JAVA_PATH=%JAVA_HOME%"
    )
)

REM Check PATH for javac
if "%JAVA_FOUND%"=="false" (
    javac -version >nul 2>&1
    if !errorlevel! == 0 (
        echo    ✓ Java JDK found in PATH
        set "JAVA_FOUND=true"
        for /f "tokens=*" %%i in ('where javac 2^>nul') do (
            set "JAVA_BIN=%%i"
            set "JAVA_PATH=!JAVA_BIN:\bin\javac.exe=!"
        )
    )
)

REM Check common JDK installation paths
if "%JAVA_FOUND%"=="false" (
    echo    Searching common JDK installation paths...
    
    set "JDK_PATHS[0]=%ProgramFiles%\Eclipse Adoptium\jdk-21*\bin\javac.exe"
    set "JDK_PATHS[1]=%ProgramFiles%\Eclipse Adoptium\jdk-17*\bin\javac.exe"
    set "JDK_PATHS[2]=%ProgramFiles%\Eclipse Adoptium\jdk-11*\bin\javac.exe"
    set "JDK_PATHS[3]=%ProgramFiles%\Java\jdk-21*\bin\javac.exe"
    set "JDK_PATHS[4]=%ProgramFiles%\Java\jdk-17*\bin\javac.exe"
    set "JDK_PATHS[5]=%ProgramFiles%\Java\jdk-11*\bin\javac.exe"
    set "JDK_PATHS[6]=%ProgramFiles%\OpenJDK\jdk-21*\bin\javac.exe"
    set "JDK_PATHS[7]=%ProgramFiles%\OpenJDK\jdk-17*\bin\javac.exe"
    set "JDK_PATHS[8]=%ProgramFiles%\OpenJDK\jdk-11*\bin\javac.exe"
    
    for /l %%i in (0,1,8) do (
        for %%j in ("!JDK_PATHS[%%i]!") do (
            if exist "%%j" (
                set "JAVA_PATH=%%~dpj.."
                set "JAVA_FOUND=true"
                echo    ✓ Java JDK found: !JAVA_PATH!
                goto :java_found
            )
        )
    )
    :java_found
)

if "%JAVA_FOUND%"=="false" (
    echo    ❌ Java JDK not found!
    echo    Please install Java JDK 11 or later
    echo    Recommended: Eclipse Temurin from https://adoptium.net/
    echo    Or Oracle JDK from https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

REM ============================================================================
REM Maven Detection and Setup
REM ============================================================================

echo [3/8] Detecting Apache Maven...

set "MAVEN_FOUND=false"

REM Check for Maven in PATH
mvn --version >nul 2>&1
if !errorlevel! == 0 (
    echo    ✓ Maven found in PATH
    set "MAVEN_FOUND=true"
    set "MAVEN_CMD=mvn"
) else (
    REM Check for Maven Wrapper
    if exist "mvnw.cmd" (
        echo    ✓ Maven Wrapper found
        set "MAVEN_FOUND=true"
        set "MAVEN_CMD=mvnw.cmd"
    ) else if exist "mvnw" (
        echo    ✓ Maven Wrapper found (Unix style)
        set "MAVEN_FOUND=true"
        set "MAVEN_CMD=mvnw"
    )
)

REM Check common Maven installation paths
if "%MAVEN_FOUND%"=="false" (
    echo    Searching for Maven installation...
    
    set "MAVEN_PATHS[0]=%ProgramFiles%\Apache\maven\bin\mvn.cmd"
    set "MAVEN_PATHS[1]=%ProgramFiles(x86)%\Apache\maven\bin\mvn.cmd"
    set "MAVEN_PATHS[2]=C:\apache-maven\bin\mvn.cmd"
    set "MAVEN_PATHS[3]=C:\tools\apache-maven\bin\mvn.cmd"
    
    for /l %%i in (0,1,3) do (
        if exist "!MAVEN_PATHS[%%i]!" (
            set "MAVEN_CMD=!MAVEN_PATHS[%%i]!"
            set "MAVEN_FOUND=true"
            echo    ✓ Maven found: !MAVEN_PATHS[%%i]!
            goto :maven_found
        )
    )
    :maven_found
)

if "%MAVEN_FOUND%"=="false" (
    echo    ❌ Maven not found!
    echo    Please install Apache Maven 3.6.3 or later
    echo    Download from: https://maven.apache.org/download.cgi
    echo    Or use the Maven Wrapper (mvnw.cmd) included in the project
    pause
    exit /b 1
)

REM ============================================================================
REM Initialize Visual Studio Environment
REM ============================================================================

echo [4/8] Initializing Visual Studio 2022 build environment...

call "%VS2022_PATH%\Common7\Tools\VsDevCmd.bat" -arch=amd64 -host_arch=amd64 >nul 2>&1
if !errorlevel! == 0 (
    echo    ✓ Visual Studio 2022 Developer Command Prompt initialized
    echo    ✓ Architecture: x64
    echo    ✓ MSVC Compiler: Available
) else (
    echo    ❌ Failed to initialize VS 2022 environment
    pause
    exit /b 1
)

REM ============================================================================
REM Project Validation and Analysis
REM ============================================================================

echo [5/8] Validating project structure and dependencies...

REM Check for essential project files
echo    Validating project files...
if not exist "pom.xml" (
    echo    ❌ pom.xml not found - Maven project structure required
    pause
    exit /b 1
)
echo    ✓ pom.xml found

if not exist "src\main\java" (
    echo    ❌ src\main\java directory not found
    pause
    exit /b 1
)
echo    ✓ Source directory structure validated

REM Check for main application class
echo    Searching for main application classes...
set "MAIN_CLASSES_FOUND=0"
for /r "src\main\java" %%f in (*.java) do (
    findstr /l "public static void main" "%%f" >nul 2>&1
    if !errorlevel! == 0 (
        echo    ✓ Main class found: %%~nxf
        set /a MAIN_CLASSES_FOUND+=1
    )
)

if !MAIN_CLASSES_FOUND! == 0 (
    echo    ⚠ No main classes found - this may be a library project
)

echo    ✓ Project structure validation complete

REM ============================================================================
REM Clean and Prepare Build Environment
REM ============================================================================

echo [6/8] Cleaning and preparing build environment...

echo    Cleaning previous build artifacts...
if exist "target" (
    rmdir /s /q "target" 2>nul
    echo    ✓ Target directory cleaned
)

if exist "*.log" (
    del /q "*.log" 2>nul
    echo    ✓ Log files cleaned
)

if exist "*.tmp" (
    del /q "*.tmp" 2>nul
    echo    ✓ Temporary files cleaned
)

REM Create necessary directories
echo    Creating build directories...
mkdir "target\logs" 2>nul
mkdir "target\reports" 2>nul
mkdir "target\dist" 2>nul
echo    ✓ Build directories created

REM ============================================================================
REM Maven Build Process
REM ============================================================================

echo [7/8] Starting Maven build process...
echo.

set "BUILD_START_TIME=%time%"
set "BUILD_LOG=target\logs\build-%date:~-4%-%date:~3,2%-%date:~0,2%-%time:~0,2%%time:~3,2%%time:~6,2%.log"

echo PolyType Build Started: %date% %time% > "%BUILD_LOG%"
echo ================================================================ >> "%BUILD_LOG%"

echo    Phase 1: Validating dependencies and downloading if needed...
echo %MAVEN_CMD% validate dependency:resolve >> "%BUILD_LOG%" 2>&1
%MAVEN_CMD% validate dependency:resolve >> "%BUILD_LOG%" 2>&1
if !errorlevel! neq 0 (
    echo    ❌ Dependency validation failed
    echo    Check build log: %BUILD_LOG%
    pause
    exit /b 1
)
echo    ✓ Dependencies validated and resolved

echo    Phase 2: Compiling source code...
%MAVEN_CMD% compile >> "%BUILD_LOG%" 2>&1
if !errorlevel! neq 0 (
    echo    ❌ Compilation failed
    echo    Check build log: %BUILD_LOG%
    type "%BUILD_LOG%" | findstr /i "error"
    pause
    exit /b 1
)
echo    ✓ Source code compiled successfully

echo    Phase 3: Running tests...
%MAVEN_CMD% test >> "%BUILD_LOG%" 2>&1
if !errorlevel! neq 0 (
    echo    ⚠ Some tests failed - continuing with build
    echo    Check test results in: target\surefire-reports\
) else (
    echo    ✓ All tests passed
)

echo    Phase 4: Packaging application...
%MAVEN_CMD% package >> "%BUILD_LOG%" 2>&1
if !errorlevel! neq 0 (
    echo    ❌ Packaging failed
    echo    Check build log: %BUILD_LOG%
    pause
    exit /b 1
)
echo    ✓ Application packaged successfully

echo    Phase 5: Creating distribution artifacts...
if exist "target\*.jar" (
    copy "target\*.jar" "target\dist\" >nul 2>&1
    echo    ✓ JAR files copied to distribution directory
    
    REM Create a run script for easy execution
    echo @echo off > "target\dist\run-polytype.bat"
    echo echo Starting PolyType Code Migrator... >> "target\dist\run-polytype.bat"
    echo java -jar polytype-migrator-*.jar %%* >> "target\dist\run-polytype.bat"
    echo    ✓ Run script created
)

REM ============================================================================
REM Build Summary and Verification
REM ============================================================================

echo [8/8] Build verification and summary...
echo.

set "BUILD_END_TIME=%time%"
echo Build completed: %BUILD_END_TIME% >> "%BUILD_LOG%"

echo ================================================================================
echo BUILD COMPLETED SUCCESSFULLY!
echo ================================================================================

echo Project Information:
echo    Build Tool: Apache Maven with Visual Studio 2022 integration
echo    Java Version: 
java -version 2>&1 | findstr "version"
echo    Build Started: %BUILD_START_TIME%
echo    Build Finished: %BUILD_END_TIME%
echo    Build Log: %BUILD_LOG%

echo.
echo Generated Artifacts:
if exist "target\*.jar" (
    for %%f in ("target\*.jar") do (
        echo    ✓ %%~nxf (%%~zf bytes)
    )
)

echo.
echo Distribution Files:
if exist "target\dist" (
    echo    Location: target\dist\
    for %%f in ("target\dist\*") do (
        echo    - %%~nxf
    )
)

echo.
echo Quick Start:
echo    1. Navigate to: target\dist\
echo    2. Run: run-polytype.bat --help
echo    3. Example: run-polytype.bat -i source.py -o output\ -l python -t java
echo.

REM ============================================================================
REM Optional: Launch application for testing
REM ============================================================================

echo ================================================================================
echo LAUNCH OPTIONS
echo ================================================================================
echo.
set /p "LAUNCH_CHOICE=Launch PolyType for testing? (y/n): "

if /i "%LAUNCH_CHOICE%"=="y" (
    echo.
    echo Launching PolyType Code Migrator...
    echo Use --help to see all available options
    echo Press Ctrl+C to return to this script
    echo.
    cd target\dist
    call run-polytype.bat --help
    cd ..\..
)

echo.
echo ================================================================================
echo Visual Studio 2022 Build Process Complete!
echo.
echo Next Steps:
echo • Test the application: target\dist\run-polytype.bat --help
echo • View build logs: %BUILD_LOG%
echo • Report issues: https://github.com/yourorg/polytype/issues
echo • Documentation: README.md
echo ================================================================================
echo.

REM ============================================================================
REM Error Handling and Cleanup
REM ============================================================================

:cleanup
endlocal
echo Build script execution completed.
pause
exit /b 0

:error
echo.
echo ================================================================================
echo BUILD FAILED
echo ================================================================================
echo An error occurred during the build process.
echo Check the following:
echo 1. All required software is installed (VS 2022, Java JDK, Maven)
echo 2. Network connection for dependency downloads
echo 3. Sufficient disk space for build artifacts
echo 4. Build log for detailed error information: %BUILD_LOG%
echo.
echo For help: https://github.com/yourorg/polytype/wiki/troubleshooting
echo ================================================================================
pause
exit /b 1