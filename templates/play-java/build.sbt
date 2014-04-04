name := "play-java"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).addPlugins(PlayJava)

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs
)     
