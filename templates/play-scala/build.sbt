name := "play-scala"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).addPlugins(PlayScala)

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)     

