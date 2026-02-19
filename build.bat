@echo off
cls
echo ========================================
echo    BUILD - FicheDeControle
echo ========================================
echo.

cd /d "%~dp0"

echo [1/3] Nettoyage des builds precedents...
call gradlew.bat clean

echo.
echo [2/3] Compilation du projet...
call gradlew.bat build

echo.
echo [3/3] Creation du JAR executable...
call gradlew.bat fatJar

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo    ✅ BUILD REUSSI !
    echo ========================================
    echo.
    echo Le fichier FicheDeControle.jar a ete cree dans :
    echo   %cd%\build\libs\FicheDeControle.jar
    echo.
    echo Pour lancer l'application :
    echo   - Double-cliquez sur Lancer.bat
    echo   - OU : java -jar build\libs\FicheDeControle.jar
    echo.
) else (
    echo.
    echo ========================================
    echo    ❌ ERREUR DE BUILD
    echo ========================================
    echo.
    echo Verifiez les erreurs ci-dessus.
    echo.
)

pause
