@echo off

set p=%~dp0
set p=%p:\=/%

java -Xms512M -Xmx1024M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=256M -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9999 -Dfile.encoding=UTF8 -Dsbt.ivy.home="%~dp0..\repository" -Dplay.home="%~dp0." -Dsbt.boot.properties="file:///%p%sbt/sbt.boot.properties" -jar "%~dp0sbt\sbt-launch.jar" %*
