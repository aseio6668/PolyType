@echo off
REM DavaJava Code Migrator - Windows Launcher
setlocal
set "SCRIPT_DIR=%~dp0"
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Error: Java not found - Please install Java JDK 11+
    pause
    exit /b 1
)
if not exist "%SCRIPT_DIR%davajava-migrator.jar" (
    echo Error: davajava-migrator.jar not found
    pause
    exit /b 1
)
if "%1"=="" (
    echo DavaJava Code Migrator v1.0
    echo.
    java -jar "%SCRIPT_DIR%davajava-migrator.jar"
    echo.
    pause
) else (
    java -jar "%SCRIPT_DIR%davajava-migrator.jar" %*
)
