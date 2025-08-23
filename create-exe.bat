@echo off
echo ===============================================
echo Creating DavaJava Windows Executable
echo ===============================================

REM Method 1: Using jpackage (Java 14+) - Best option if available
java --version | findstr /C:"17" >nul
if %ERRORLEVEL% equ 0 (
    echo Using jpackage to create native Windows executable...
    jpackage --input . --name DavaJava --main-jar davajava-migrator.jar --main-class com.davajava.migrator.Main --type exe --dest dist --app-version 1.0.0 --description "DavaJava Code Migrator - Multi-language to Java converter"
    if exist "dist\DavaJava.exe" (
        echo ✅ Native executable created: dist\DavaJava.exe
        goto :end
    )
)

REM Method 2: Create a self-extracting batch/JAR hybrid
echo Creating self-contained executable batch file...
copy /b davajava.bat + davajava-migrator.jar davajava.exe >nul 2>&1
if exist "davajava.exe" (
    echo ✅ Hybrid executable created: davajava.exe
    echo Note: This is a batch/JAR hybrid that requires Java to be installed
) else (
    echo ❌ Failed to create executable
)

:end
echo.
echo ===============================================
echo Executable Creation Complete
echo ===============================================
pause