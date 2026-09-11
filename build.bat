@echo off
chcp 65001 >nul
cd /d %~dp0
rem compile all java files under src\net\wanhe\edusystem into net\
dir /s /b src\net\wanhe\edusystem\*.java > filelist.txt
javac -encoding UTF-8 -cp "lib\*" -d net @filelist.txt
if errorlevel 1 goto :failed
del filelist.txt
echo build ok
goto :eof
:failed
del filelist.txt
echo build failed
exit /b 1
