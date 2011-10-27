@echo off

echo [info] Clean Ivy cache.

del "%~dp0..\repository\cache\*.xml"
del "%~dp0..\repository\cache\*.properties"
rd /S /Q "%~dp0..\repository\cache\play"
