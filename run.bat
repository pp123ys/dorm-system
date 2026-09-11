@echo off
cd /d %~dp0
call build.bat
java -Dfile.encoding=UTF-8 -Dstdin.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp ".;net;lib\*" net.wanhe.dorm.Run
