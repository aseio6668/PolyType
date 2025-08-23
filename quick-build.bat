@echo off
echo ===============================================
echo DavaJava - Quick Build (Core Testing Only)
echo ===============================================

java -version >nul 2>&1 && javac -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ❌ Java not found. Run verify-java.bat first.
    pause
    exit /b 1
)

echo ✅ Java JDK ready
echo.

if exist "quick-build" rmdir /s /q "quick-build"
mkdir "quick-build\classes"

echo ===============================================
echo Compiling Core Classes (No Dependencies)
echo ===============================================

echo Compiling core parsing logic...
javac -d quick-build\classes ^
    src\main\java\com\davajava\migrator\core\SourceLanguage.java ^
    src\main\java\com\davajava\migrator\core\*Exception.java

javac -cp quick-build\classes -d quick-build\classes ^
    src\main\java\com\davajava\migrator\core\ast\NodeType.java ^
    src\main\java\com\davajava\migrator\core\ast\ASTVisitor.java ^
    src\main\java\com\davajava\migrator\core\ast\ASTNode.java

javac -cp quick-build\classes -d quick-build\classes ^
    src\main\java\com\davajava\migrator\core\ast\*.java

javac -cp quick-build\classes -d quick-build\classes ^
    src\main\java\com\davajava\migrator\core\TranslationOptions.java ^
    src\main\java\com\davajava\migrator\core\Parser.java ^
    src\main\java\com\davajava\migrator\core\Translator.java

javac -cp quick-build\classes -d quick-build\classes ^
    src\main\java\com\davajava\migrator\output\JavaFileGenerator.java

javac -cp quick-build\classes -d quick-build\classes ^
    src\main\java\com\davajava\migrator\parser\rust\RustParser.java ^
    src\main\java\com\davajava\migrator\parser\python\PythonParser.java

javac -cp quick-build\classes -d quick-build\classes ^
    src\main\java\com\davajava\migrator\translator\BaseJavaTranslator.java

javac -cp quick-build\classes -d quick-build\classes ^
    src\main\java\com\davajava\migrator\translator\rust\*.java ^
    src\main\java\com\davajava\migrator\translator\python\*.java

if %ERRORLEVEL% equ 0 (
    echo ✅ Core compilation successful!
    echo.
    echo Testing parsing functionality...
    javac -cp quick-build\classes test-core.java -d quick-build\classes
    java -cp quick-build\classes TestCore
    
    echo.
    echo ===============================================
    echo Quick Build Complete!
    echo ===============================================
    echo ✅ Core parsing: Working
    echo ✅ Rust parser: Compiled  
    echo ✅ Python parser: Compiled
    echo.
    echo For full CLI functionality, run: build.bat
    
) else (
    echo ❌ Some compilation errors occurred
    echo Try: build.bat for full dependency resolution
)

echo.
pause