@echo off
rem Keep this file ASCII-only. Chinese text in a .bat needs a matching console
rem codepage, and a UTF-8 comment byte followed by '>' breaks cmd's parser
rem (the '>' in "dir > filelist.txt" below would be eaten as a UTF-8 trail byte).
rem Detailed Chinese notes live in BUILD.md.
chcp 65001 >nul
cd /d %~dp0

rem Remove stale output so the build only reflects the current sources.
if exist net rmdir /s /q net
md net

rem JdbcUtil now uses Spring's DataSourceUtils so the same DAO+Service code works in both
rem the console app and the Web app; those 5 Spring jars are not versioned (too large), so
rem hydrate them from the local Maven repository first. The script is a no-op when present.
call fetch-lib.bat
if errorlevel 1 goto :failed

rem Collect source files with for /f instead of "dir /s /b > filelist.txt":
rem   dir redirection writes the list in the current console encoding (UTF-8 here)
rem   while javac reads an @argfile in the platform default encoding (GBK), so any
rem   non-ASCII path (e.g. C:\Users\<chinese>\...) is read as mojibake and javac
rem   answers "file not found".
rem   PowerShell below prints ONLY relative paths under src (pure ASCII), so the
rem   list is encoding-independent, and cmd splits the javac call automatically
rem   when the command line gets long.
rem The redirect to nul matters: without it the PowerShell process inherits and
rem consumes whatever is on stdin, which would eat the keyboard/redirected input of
rem the program that run.bat launches right afterwards.
set "SOURCES="
for /f "usebackq delims=" %%f in (`powershell -NoProfile -Command "$root = (Get-Location).Path; Get-ChildItem -Recurse -File -Filter *.java -Path src | ForEach-Object { $_.FullName.Substring($root.Length + 1) }" ^<nul`) do call set "SOURCES=%%SOURCES%% %%f"
if not defined SOURCES goto :failed

javac -encoding UTF-8 -cp "lib\*" -d net %SOURCES%
if errorlevel 1 goto :failed

echo build ok
goto :eof

:failed
echo build failed
exit /b 1
