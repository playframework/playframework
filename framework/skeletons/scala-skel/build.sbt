name := "%APPLICATION_NAME%"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)     

play.Project.playScalaSettings
