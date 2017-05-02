//
// Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
//

name := "dist-sample"
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies += guice

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.11.8")
scalacOptions +="-Xfatal-warnings"

routesGenerator := InjectedRoutesGenerator
