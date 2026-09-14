@echo off
rem Keep this file ASCII-only; see the note at the top of build.bat.
rem
rem Why this file exists:
rem   util/JdbcUtil.java now uses Spring's DataSourceUtils / DataSourceTransactionManager so that
rem   the same DAO+Service code works both in the console app and in the Web app.
rem   That means the console app needs 5 Spring jars on its classpath (lib\ is on the classpath),
rem   but those jars are NOT in version control (too large). This script copies them out of the
rem   local Maven repository, so a fresh clone can build the console app offline.
rem
rem Usage:  fetch-lib.bat        (copy missing jars)
rem         fetch-lib.bat -force (re-copy all)
rem
rem If the jars are missing from the Maven repository too, run once:
rem   build-web.bat        (it downloads the same Spring versions via Maven)
setlocal enabledelayedexpansion

set "SPRING_VER=5.3.31"
set "REPO=%USERPROFILE%\.m2\repository\org\springframework"
set "FORCE=%~1"

cd /d %~dp0
if not exist lib md lib

set "MISSING="
for %%j in (spring-jdbc spring-tx spring-core spring-beans spring-jcl) do (
    set "SRC=%REPO%\%%j\%SPRING_VER%\%%j-%SPRING_VER%.jar"
    set "DST=lib\%%j-%SPRING_VER%.jar"
    if /i "%FORCE%"=="-force" (
        if exist "!SRC!" ( copy /y "!SRC!" "!DST!" >nul & echo [copy] %%j-%SPRING_VER%.jar )
    ) else (
        if not exist "!DST!" (
            if exist "!SRC!" (
                copy /y "!SRC!" "!DST!" >nul
                echo [copy] %%j-%SPRING_VER%.jar
            ) else (
                echo [MISS] %%j-%SPRING_VER%.jar  -- not found in %REPO%
                set "MISSING=1"
            )
        )
    )
)

if defined MISSING (
    echo.
    echo [ERROR] Some Spring jars are missing from the local Maven repository.
    echo         Run "build-web.bat" first to download them, then run this script again.
    exit /b 1
)

echo lib is ready.
exit /b 0
