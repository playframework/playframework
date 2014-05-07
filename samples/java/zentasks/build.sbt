name := "zentask"

version := "1.0"

libraryDependencies ++= Seq(javaJdbc, javaEbean)     

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.10.4")