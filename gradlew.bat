@echo off
setlocal EnableExtensions
cd /d "%~dp0"
where gradle >nul 2>nul
if %errorlevel%==0 (
  gradle %*
  exit /b %errorlevel%
)
if not defined JAVA_HOME (
  where javac >nul 2>nul
  if errorlevel 1 (echo JDK 17+ is required. Install a JDK and add javac to PATH.& exit /b 1)
) else (
  set "PATH=%JAVA_HOME%\bin;%PATH%"
)
set "TASKS=%*"
if "%TASKS%"=="clean" goto clean
if not exist build\classes\java\main mkdir build\classes\java\main
if not exist build\classes\java\test mkdir build\classes\java\test
if exist build\classes\java\main rmdir /s /q build\classes\java\main
if exist build\classes\java\test rmdir /s /q build\classes\java\test
if exist build\main-sources.txt del build\main-sources.txt
if exist build\test-sources.txt del build\test-sources.txt
mkdir build\classes\java\main
mkdir build\classes\java\test
for /r src\main\java %%F in (*.java) do echo "%%F" >> build\main-sources.txt
javac --release 17 -encoding UTF-8 -d build\classes\java\main @build\main-sources.txt
if errorlevel 1 exit /b 1
if not "%TASKS:test=%"=="%TASKS%" goto test
if not "%TASKS:logicTest=%"=="%TASKS%" goto test
if not "%TASKS:check=%"=="%TASKS%" goto test
if "%TASKS:runGame=%"=="%TASKS%" if "%TASKS:run=%"=="%TASKS%" goto build
if not "%TASKS:runGame=%"=="%TASKS%" goto run
if not "%TASKS:run=%"=="%TASKS%" goto run
:build
echo BUILD SUCCESSFUL (offline JDK fallback; Gradle itself is not installed)
exit /b 0
:test
for /r src\test\java %%F in (*.java) do echo "%%F" >> build\test-sources.txt
javac --release 17 -encoding UTF-8 -cp build\classes\java\main -d build\classes\java\test @build\test-sources.txt
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp "build\classes\java\main;build\classes\java\test" com.lostvault.GameLogicTest
exit /b %errorlevel%
:run
java -cp build\classes\java\main com.lostvault.Main
exit /b %errorlevel%
:clean
if exist build rmdir /s /q build
echo CLEAN SUCCESSFUL
exit /b 0
