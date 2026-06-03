@rem 🍓 FRAISE Android Gradle Direct Delegate Wrapper (Windows)
@echo off
where gradle >nul 2>nul
if %ERRORLEVEL% equ 0 (
  gradle %*
) else (
  echo ❌ Error: 'gradle' command not found in current PATH.
  exit /b 1
)
