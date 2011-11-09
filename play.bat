:begin
@echo off

set p=%~dp0
set p=%p:\=/%

if exist "conf\application.conf" goto existingApplication

:noApplication
java -Dsbt.ivy.home=%~dp0repository -Dplay.home=%~dp0framework -Dsbt.boot.properties="file:///%p%sbt/play.boot.properties" -jar %~dp0framework\sbt\sbt-launch-0.11.0.jar %*

goto end

:existingApplication
if not "%1" == "clean" goto runCommand

:cleanCache
call %~dp0framework\cleanIvyCache.bat

:runCommand
if "%1" == "" goto enterConsole

call %~dp0framework\build.bat %*
goto end

:enterConsole

call %~dp0framework\build.bat play 

:end
