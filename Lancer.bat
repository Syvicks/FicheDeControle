@echo off
cd /d "%~dp0"

REM Vérifier si Java est installé
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ========================================
    echo    ❌ ERREUR : Java non installé
    echo ========================================
    echo.
    echo Java est requis pour lancer l'application.
    echo Veuillez installer Java 11 ou supérieur.
    echo.
    pause
    exit /b 1
)

REM Créer le dossier logs s'il n'existe pas
if not exist logs mkdir logs

REM Options JVM pour VM Citrix (mémoire limitée)
set JVM_OPTS=-Xmx512m -Xms256m

REM Lancer l'application
if exist FicheDeControle.jar (
    echo ========================================
    echo    🚀 Lancement de FicheDeControle
    echo ========================================
    echo.
    java %JVM_OPTS% -jar FicheDeControle.jar
) else if exist build\libs\FicheDeControle.jar (
    echo ========================================
    echo    🚀 Lancement de FicheDeControle (dev)
    echo ========================================
    echo.
    java %JVM_OPTS% -jar build\libs\FicheDeControle.jar
) else (
    echo ========================================
    echo    ❌ ERREUR : JAR introuvable
    echo ========================================
    echo.
    echo Le fichier FicheDeControle.jar n'a pas été trouvé.
    echo.
    echo Veuillez d'abord compiler le projet avec :
    echo   build.bat
    echo.
    pause
    exit /b 1
)
