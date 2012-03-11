@echo off

set PLAY_VERSION="2.0-SNAPSHOT"

if defined JPDA_PORT set DEBUG_PARAM="-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=%JPDA_PORT%"

set p=%~dp0
set p=%p:\=/%

java -Xms512M -Xmx1024M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=256M %DEBUG_PARAM% -Dfile.encoding=UTF8 -Dplay.version="%PLAY_VERSION%" -Dsbt.ivy.home="%~dp0..\repository" -Dplay.home="%~dp0." -Dsbt.boot.properties="file:///%p%sbt/sbt.boot.properties" -jar "%~dp0sbt\sbt-launch.jar" %*
