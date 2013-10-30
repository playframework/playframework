name := "%APPLICATION_NAME%"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  javaWs,
  cache
)

play.Project.playJavaSettings
