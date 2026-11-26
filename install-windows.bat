@echo off
setlocal

set "JAR_NAME=hatrix-1.0.0.jar"
set "MODS_DIR=%APPDATA%\.minecraft\mods"

echo Hatrix installer (Windows)
echo.

if not exist "%~dp0%JAR_NAME%" (
    echo ERROR: Could not find %JAR_NAME% next to this script.
    echo Make sure install-windows.bat and %JAR_NAME% are in the same folder.
    echo ^(if you downloaded them separately, put them together first^)
    pause
    exit /b 1
)

if not exist "%MODS_DIR%" (
    echo Creating mods folder: %MODS_DIR%
    mkdir "%MODS_DIR%"
)

copy /Y "%~dp0%JAR_NAME%" "%MODS_DIR%\%JAR_NAME%" >nul
if errorlevel 1 (
    echo Failed to copy the jar. Try running this script as Administrator.
    pause
    exit /b 1
)

echo.
echo Installed to: %MODS_DIR%\%JAR_NAME%
echo.
echo This script only copies the mod itself. You still need, separately:
echo   - Minecraft 26.3 with Fabric Loader 0.20.1+  (https://fabricmc.net/use/)
echo   - Fabric API 0.167.0+26.3, as its own jar in the same mods folder
echo   - Java 25
echo.
echo Once those are in place, launch Minecraft with the "fabric-loader" profile.
echo.
pause
