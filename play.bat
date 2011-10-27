:begin
@echo off

if exist "conf\application.conf" goto existingApplication

:noApplication
java -cp "%~dp0framework/sbt/boot/scala-2.9.1/lib/*;%~dp0framework/sbt/boot/scala-2.9.1/org.scala-tools.sbt/sbt/0.11.0/*;%~dp0repository/local/play/play_2.9.1/2.0/jars/*" -Dsbt.ivy.home="%~dp0repository" play.console.Console %*

goto end

:existingApplication
if not "%1" == "clean" goto enterConsole

:cleanCache
call %~dp0framework\cleanIvyCache.bat

:enterConsole
call %~dp0framework\build.bat play %*

:end
