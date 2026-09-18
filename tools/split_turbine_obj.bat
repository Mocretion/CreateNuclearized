@echo off
REM Regenerates the rotor/static OBJ splits. The py uses paths relative to the repo root, so cd there first (this bat lives in tools\).
cd /d "%~dp0.."
python tools\split_turbine_obj.py
if errorlevel 1 (
    echo.
    echo Split failed. Is Python on PATH?
    pause
    exit /b 1
)
echo.
echo Done.
