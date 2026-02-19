@echo off
:loop
cls
echo ========================================
echo    Mode Développement - FicheDeControle
echo ========================================
echo.
echo Compilation et lancement...
echo.

REM Compiler et lancer
call gradlew.bat run --no-daemon

echo.
echo ========================================
echo    Application fermée
echo ========================================
echo.
echo Appuyez sur une touche pour relancer...
echo Ou Ctrl+C pour quitter
pause >nul

goto loop
