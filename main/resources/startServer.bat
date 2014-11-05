@ECHO OFF
TITLE SBS Ç°ÖÃ·þÎñÆ÷
setlocal

set JAVA_HOME=D:\Java\jdk1.7.0_17_i586
set Path=%JAVA_HOME%\bin;..\lib;%Path%

for %%i in ("..\lib\*.jar") do call :append "%%i"
goto okenv

:append
set CLASSPATH=%CLASSPATH%;%1
goto :eof

:okenv
set CLASSPATH=%CLASSPATH%;../classes
set CLASSPATH
java -cp %CLASSPATH% -Dfile.encoding=GBK  org.fbi.sbspreserver.SpsServer

:eof
endlocal
