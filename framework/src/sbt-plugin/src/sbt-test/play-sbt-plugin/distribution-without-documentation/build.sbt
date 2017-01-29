//
// Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
//

name := "dist-no-documentation-sample"

// actually it should fail on any warning so that we can check that packageBin won't include any documentation
scalacOptions in Compile := Seq("-Xfatal-warnings", "-deprecation")

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies += guice

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.11.8")

play.sbt.PlayImport.PlayKeys.includeDocumentationInBinary := false

packageDoc in Compile := { new File(".") }