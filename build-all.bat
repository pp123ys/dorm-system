@echo off
rem Keep this file ASCII-only; see the note at the top of build.bat.
rem
rem Builds a SINGLE-PORT deployment: the Vue build output is embedded into the Spring Boot jar,
rem so http://localhost:8080 serves both the pages and the /api endpoints (no Vite server needed).
rem
rem The plain "build-web.bat" deliberately does NOT do this: embedding the frontend would add
rem the dist folder to every jar and rebuild it on every frontend change, which is slower while
rem developing. Use whichever fits the situation.
rem
rem Usage:  build-all.bat
setlocal

cd /d %~dp0

echo [1/3] building frontend (npm run build)
pushd web-frontend
call npm run build
if errorlevel 1 (
    popd
    echo [ERROR] npm run build failed
    exit /b 1
)
popd

echo [2/3] copying dist into web\src\main\resources\static
if exist "web\src\main\resources\static" rmdir /s /q "web\src\main\resources\static"
xcopy /e /i /y /q "web-frontend\dist" "web\src\main\resources\static" >nul
if errorlevel 1 (
    echo [ERROR] failed to copy dist into resources\static
    exit /b 1
)

echo [3/3] packaging jar (frontend included)
call build-web.bat
if errorlevel 1 (
    echo [ERROR] maven package failed
    exit /b 1
)

echo.
echo done. start it with:
echo   java -Dfile.encoding=UTF-8 -jar web\target\dorm-web-1.0.0.jar
echo then open http://localhost:8080
echo.
echo note: web\src\main\resources\static is build output -- clean it with:
echo   rmdir /s /q web\src\main\resources\static
exit /b 0
