@echo off
rem Keep this file ASCII-only; see the note at the top of build.bat.
rem
rem Why this file exists:
rem   Maven on this machine picks up C:\Program Files\Java\jre1.8.0_191 as its JVM
rem   (there is no JAVA_HOME set), and a JRE has no javac, so a plain "mvn package"
rem   fails with "No compiler is provided in this environment. Perhaps you are
rem   running on a JRE rather than a JDK?".
rem   Pointing JAVA_HOME at the JDK fixes it without touching machine-wide settings.
rem
rem Usage:  build-web.bat              (package the Spring Boot fat jar)
rem         build-web.bat clean package -DskipTests
setlocal

set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_191"
if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo [ERROR] JDK not found at "%JAVA_HOME%"
    echo         Edit JAVA_HOME in this file to point at your JDK.
    exit /b 1
)

cd /d %~dp0
echo [INFO] using JAVA_HOME=%JAVA_HOME%

if "%~1"=="" (
    call mvn -f web\pom.xml -B clean package -DskipTests
) else (
    call mvn -f web\pom.xml -B %*
)
exit /b %errorlevel%
