@echo off
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
    echo Java is not installed yet.
    echo Opening the download page: install it, then double-click this file again.
    start https://adoptium.net/
    pause
    exit /b
)
java -jar AutoFlappy.jar
pause
