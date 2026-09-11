@echo off
chcp 65001 >nul
cd /d %~dp0
call build.bat
java -Dfile.encoding=UTF-8 -cp ".;net;lib\*" net.wanhe.edusystem.Run < test_input.txt
