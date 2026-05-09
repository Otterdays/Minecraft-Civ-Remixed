:: launch_optimizer.bat
@echo off
cd /d "%~dp0"

echo ======================================
echo Imagery Optimizery
echo ======================================
echo.

python --version >nul 2>&1

IF %ERRORLEVEL% NEQ 0 (
    echo Python is not installed or not in PATH.
    pause
    exit /b
)

echo Installing/updating Pillow...
pip install pillow

echo.
echo Launching GUI...
python optimize_pngs.py --gui

echo.
echo Finished.
pause