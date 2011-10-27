@echo off

set p=%~dp0
set p=%p:\=/%

java -Xms512M -Dfile.encoding=UTF8  -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=256m -Dsbt.ivy.home="%~dp0..\repository" -Dplay.home="%~dp0." -Dsbt.boot.properties="file:///%p%sbt/sbt.boot.properties" -jar "%~dp0sbt\sbt-launch-0.11.0.jar" %*