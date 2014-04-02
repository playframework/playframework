/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import Keys._
import com.typesafe.sbt.packager.Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName = "secret-sample"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
  )

  val Secret = """(?s).*application.secret="(.*)".*""".r

  val main = play.Project(appName, appVersion, appDependencies).settings(
    TaskKey[Unit]("check-secret") := {
      val file = IO.read(baseDirectory.value / "conf/application.conf")
      file match {
        case Secret("changeme") => throw new RuntimeException("secret not changed!!\n" + file)
        case Secret(_) =>
        case _ => throw new RuntimeException("secret not found!!\n" + file)
      }
    }
  )

}
