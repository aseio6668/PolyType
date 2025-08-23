@echo off
echo ===============================================
echo Java Installation Verification
echo ===============================================

echo Checking Java Runtime Environment...
java -version
if %ERRORLEVEL% neq 0 (
    echo ❌ Java runtime not found
    goto :install_help
) else (
    echo ✅ Java runtime found
)

echo.
echo Checking Java Compiler...
javac -version
if %ERRORLEVEL% neq 0 (
    echo ❌ Java compiler not found
    echo This means you have JRE but not JDK
    goto :install_help
) else (
    echo ✅ Java compiler found
)

echo.
echo Checking JAR tool...
jar --help >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ❌ JAR tool not found
) else (
    echo ✅ JAR tool found
)

echo.
echo Testing basic compilation...
echo public class JavaTest { public static void main(String[] args) { System.out.println("Java works!"); } } > JavaTest.java
javac JavaTest.java
if %ERRORLEVEL% equ 0 (
    echo ✅ Basic compilation works
    java JavaTest
    del JavaTest.java JavaTest.class
) else (
    echo ❌ Basic compilation failed
    del JavaTest.java 2>nul
)

echo.
echo ===============================================
echo Java Environment Summary
echo ===============================================

echo Checking environment variables...
if defined JAVA_HOME (
    echo ✅ JAVA_HOME is set: %JAVA_HOME%
) else (
    echo ⚠️  JAVA_HOME not set ^(optional but recommended^)
)

echo.
echo PATH entries containing 'java':
echo %PATH% | findstr /i java

echo.
echo ===============================================
echo Verification Complete
echo ===============================================

java -version >nul 2>&1 && javac -version >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo 🎉 Your Java installation is ready for DavaJava!
    echo.
    echo You can now run:
    echo   simple-java-build.bat    ^(for quick core build^)
    echo   java-only-build.bat      ^(for full build with dependencies^)
    echo.
) else (
    echo ❌ Java setup needs attention
    goto :install_help
)

goto :end

:install_help
echo.
echo ===============================================
echo Java Installation Help
echo ===============================================
echo.
echo You need Java JDK 11 or higher ^(not just JRE^)
echo.
echo Download options:
echo 1. Eclipse Temurin: https://adoptium.net/temurin/releases/
echo 2. Oracle JDK: https://www.oracle.com/java/technologies/downloads/
echo 3. OpenJDK: https://openjdk.org/
echo.
echo Installation tips:
echo 1. Download JDK ^(not JRE^) version 11 or higher
echo 2. During installation, check "Add to PATH" if available
echo 3. After installation, restart command prompt
echo 4. Run this script again to verify
echo.
echo Manual PATH setup ^(if needed^):
echo 1. Find your Java installation ^(usually C:\Program Files\Java\jdk-X.X.X^)
echo 2. Add the 'bin' folder to your PATH environment variable
echo 3. Set JAVA_HOME to the JDK root directory
echo.

:end
pause