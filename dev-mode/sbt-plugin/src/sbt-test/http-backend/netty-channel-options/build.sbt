//
// Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
//
// Help intellij-scala out...
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import play.sbt.{ PlayScala, PlayNettyServer, PlayAkkaHttpServer }
import play.sbt.PlayService.autoImport._

organization := "com.lightbend.play"
        name := "netty-channel-options"
     version := "1.0-SNAPSHOT"

enablePlugins(PlayScala, PlayNettyServer)
disablePlugins(PlayAkkaHttpServer)

               updateOptions := updateOptions.value.withLatestSnapshots(false)
                scalaVersion := sys.props.get("scala.version").getOrElse("2.12.9")
PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode
         libraryDependencies += guice
 InputKey[Unit]("callIndex") := {
   try DevModeBuild.callIndex() catch { case e: java.net.ConnectException =>
     play.sbt.run.PlayRun.stop((stagingDirectory in Universal).value)
     throw e
   }
 }
InputKey[Unit]("checkLines") := {
  val args                  = Def.spaceDelimited("<source> <target>").parsed
  val source :: target :: _ = args
  try DevModeBuild.checkLines(source, target) catch { case e: java.net.ConnectException =>
    play.sbt.run.PlayRun.stop((stagingDirectory in Universal).value)
    throw e
  }
}
