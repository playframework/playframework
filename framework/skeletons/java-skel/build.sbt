import play.Project._

name := "%APPLICATION_NAME%"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache
  )     

playJavaSettings
