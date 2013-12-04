:begin
@echo off

setlocal enabledelayedexpansion

set p=%~dp0
set p=%p:\=/%
set fp=file:///!p: =%%20!
set buildScript="%~dp0framework\build.bat"
set additionalArgs=%*
if not defined SBT_SCALA_VERSION set SBT_SCALA_VERSION=2.10.3

if exist "conf\application.conf" goto existingApplication
if exist "conf\reference.conf" goto existingApplication
if exist "project" goto existingApplication

:noApplication
java -Dsbt.ivy.home="%~dp0repository" -Dplay.home="%~dp0framework" -Dsbt.boot.properties="%fp%framework/sbt/play.boot.properties" -Dsbt.scala.version="%SBT_SCALA_VERSION%" %PLAY_OPTS% -jar "%~dp0framework\sbt\sbt-launch.jar" %*

goto end

:existingApplication
if not "%~1" == "clean-all" goto runCommand

:cleanCache
if exist "target" rmdir /s /q target
if exist "tmp" rmdir /s /q tmp
if exist "logs" rmdir /s /q logs
if exist "project\target" rmdir /s /q project\target
if exist "project\project" rmdir /s /q project\project
if exist "dist" rmdir /s /q dist

shift
set additionalArgs=%additionalArgs:*clean-all=%

if "%~1" == "" goto endWithMessage

:runCommand
if "%~1" == "" goto enterConsole

if "%~1" == "debug" goto setDebug
goto enterConsoleWithCommands

:setDebug
set JPDA_PORT=9999
shift
set additionalArgs=%additionalArgs:*debug=%

if "%~1" == "" goto enterConsole

:enterConsoleWithCommands

call %buildScript% %additionalArgs%
goto end

:enterConsole

call %buildScript% play 
goto end

:endWithMessage
echo [info] Done!

:end
endlocal
