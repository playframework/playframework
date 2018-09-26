/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

val Secret = """(?s).*play.http.secret.key="(.*)".*""".r

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)
  .settings(
    name := "secret-sample",
    version := "1.0-SNAPSHOT",
    libraryDependencies += guice,
    TaskKey[Unit]("checkSecret") := {
      val file = IO.read(baseDirectory.value / "conf/application.conf")
      file match {
        case Secret("changeme") => throw new RuntimeException("secret not changed!!\n" + file)
        case Secret(_) =>
        case _ => throw new RuntimeException("secret not found!!\n" + file)
      }
    }
  )