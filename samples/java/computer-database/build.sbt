name := "computer-database"

version := "1.0"

libraryDependencies ++= Seq(javaJdbc, javaEbean)

lazy val root = (project in file(".")).addPlugins(PlayJava)
