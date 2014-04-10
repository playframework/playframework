name := "computer-database"

version := "1.0"

libraryDependencies ++= Seq(jdbc, anorm)

lazy val root = (project in file(".")).addPlugins(PlayScala)
