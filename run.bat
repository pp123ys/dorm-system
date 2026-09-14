@echo off
rem Keep this file ASCII-only; see the note at the top of build.bat.
rem chcp 65001 makes the console UTF-8 so the Chinese menus print correctly and
rem Chinese typed on the keyboard reaches Scanner in UTF-8 as well.
chcp 65001 >nul
cd /d %~dp0

rem Do not start the program when the build failed: the JVM would otherwise run
rem stale .class files left over from an earlier successful build.
call build.bat
if errorlevel 1 (
    echo build failed, startup cancelled
    exit /b 1
)

rem -Dfile.encoding=UTF-8 is the load-bearing flag on this machine: the JDK here is
rem 1.8.0_191, which predates -Dstdin/-Dstdout/-Dstderr.encoding (JDK 18+), so those
rem three are simply ignored and only -Dfile.encoding takes effect. Java 8 derives
rem both the stdin decoder and the stdout/stderr encoders from it, which is why it
rem must be present: without it the JVM uses GBK, the Chinese menus come out garbled
rem and Chinese input ends up in the database as mojibake.
java -Dfile.encoding=UTF-8 -Dstdin.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp ".;net;lib\*" net.wanhe.dormsystem.Run
exit /b %errorlevel%
