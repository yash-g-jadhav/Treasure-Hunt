@echo off
where gradle >nul 2>nul
if %errorlevel%==0 (gradle %*) else (echo Gradle is not installed. Install Gradle 8+ to run this project on Windows. & exit /b 1)
