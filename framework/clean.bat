@echo off

echo [info] Cleaning target directories...

if exist "%~dp0target"                    rmdir /q /s "%~dp0target"
if exist "%~dp0project\project"           rmdir /q /s "%~dp0project\project"
if exist "%~dp0project\target"            rmdir /q /s "%~dp0project\target"
if exist "%~dp0logs"                      rmdir /q /s "%~dp0logs"
if exist "%~dp0src\anorm\target"          rmdir /q /s "%~dp0src\anorm\target"
if exist "%~dp0src\play\target"           rmdir /q /s "%~dp0src\play\target"
if exist "%~dp0src\templates\target"      rmdir /q /s "%~dp0src\templates\target"
if exist "%~dp0src\sbt-plugin\target"     rmdir /q /s "%~dp0src\sbt-plugin\target"
if exist "%~dp0src\console\target"        rmdir /q /s "%~dp0src\console\target"
if exist "%~dp0sbt\boot\scala-2.9.1\play" rmdir /q /s "%~dp0sbt\boot\scala-2.9.1\play"