echo ===============================================
echo DavaJava Code Migrator - Windows Distribution
echo ===============================================
echo.
echo Creating Windows-friendly distribution...
echo.

REM Copy files to a distribution folder
if exist "dist" rmdir /s /q "dist"
mkdir "dist"
copy "davajava-migrator.jar" "dist\"
copy "DavaJava.bat" "dist\"
copy "examples\*" "dist\examples\" 2>nul
if not exist "dist\examples" mkdir "dist\examples"
copy "examples\*.rs" "dist\examples\" >nul 2>&1
copy "examples\*.py" "dist\examples\" >nul 2>&1

REM Create a README for Windows users
echo # DavaJava Code Migrator - Windows Distribution > "dist\README-Windows.txt"
echo. >> "dist\README-Windows.txt"
echo ## Quick Start: >> "dist\README-Windows.txt"
echo 1. Double-click DavaJava.bat to see usage >> "dist\README-Windows.txt"
echo 2. Or run from command line: >> "dist\README-Windows.txt"
echo    DavaJava.bat examples\example.rs output rust >> "dist\README-Windows.txt"
echo    DavaJava.bat -r examples output rust >> "dist\README-Windows.txt"
echo. >> "dist\README-Windows.txt"
echo ## Requirements: >> "dist\README-Windows.txt"
echo - Java JDK 11 or later installed and in PATH >> "dist\README-Windows.txt"
echo. >> "dist\README-Windows.txt"

echo ✅ Windows distribution created in dist\ folder
echo.
echo The distribution contains:
echo   - DavaJava.bat (Windows launcher)
echo   - davajava-migrator.jar (Java application)
echo   - examples\ (sample files)
echo   - README-Windows.txt (instructions)
echo.
pause
