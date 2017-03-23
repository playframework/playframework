@echo off

echo [info] Cleaning everything...

%~dp0clean.bat
if exist "%~dp0..\repository" rmdir /q /s "%~dp0..\repository"
if exist "%~dp0sbt\boot"      rmdir /q /s "%~dp0sbt\boot"

echo [info] Done!