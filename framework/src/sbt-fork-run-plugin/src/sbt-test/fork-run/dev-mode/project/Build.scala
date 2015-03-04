/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

import sbt._
import sbt.Keys._

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.util.Properties

object DevModeBuild {

  object DevModeKeys {
    val writeRunProperties = TaskKey[Unit]("write-run-properties")
    val waitForServer = TaskKey[Unit]("wait-for-server")
    val resetReloads = TaskKey[Unit]("reset-reloads")
    val verifyReloads = InputKey[Unit]("verify-reloads")
    val verifyResourceContains = InputKey[Unit]("verify-resource-contains")
  }

  def settings: Seq[Setting[_]] = Seq(
    DevModeKeys.writeRunProperties := {
      IO.write(file("run.properties"), s"project.version=${play.core.PlayVersion.current}")
    },

    DevModeKeys.waitForServer := {
      DevModeBuild.waitForServer()
    },

    DevModeKeys.resetReloads := {
      (target.value / "reload.log").delete()
    },

    DevModeKeys.verifyReloads := {
      val expected = Def.spaceDelimited().parsed.head.toInt
      val actual = try IO.readLines(target.value / "reload.log").count(_.nonEmpty)
      catch {
        case _: java.io.IOException => 0
      }
      if (expected == actual) {
        println(s"Expected and got $expected reloads")
      } else {
        throw new RuntimeException(s"Expected $expected reloads but got $actual")
      }
    },

    DevModeKeys.verifyResourceContains := {
      val args = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path = args.head
      val status = args.tail.head.toInt
      val assertions = args.tail.tail
      DevModeBuild.verifyResourceContains(path, status, assertions, 0)
    }
  )

  val ServerMaxAttempts = 3 * 60
  val ServerWaitTime = 1000l

  @tailrec
  def waitForServer(attempts: Int = 0): Unit = {
    println(s"Connecting to server: attempt $attempts")
    try {
      val url = new java.net.URL("http://localhost:9000")
      val connection = url.openConnection().asInstanceOf[java.net.HttpURLConnection]
      connection.connect()
      connection match {
        case h: java.net.HttpURLConnection =>
          println(s"Server gave us status ${h.getResponseCode} ${h.getResponseMessage}")
        if (h.getResponseCode != 200)
          throw new Exception(s"Bad response code ${h.getResponseCode} from server")
        case _ =>
          println(s"Not an HttpURLConnection? ${connection.getClass.getName}")
      }
      connection.disconnect()
    } catch {
      case e: Exception =>
        if (attempts < ServerMaxAttempts) {
          Thread.sleep(ServerWaitTime)
          waitForServer(attempts + 1)
        } else {
          println(s"After $attempts attempts:")
          throw e
        }
    }
  }

  val MaxAttempts = 10
  val WaitTime = 500l
  val ConnectTimeout = 10000
  val ReadTimeout = 10000

  @tailrec
  def verifyResourceContains(path: String, status: Int, assertions: Seq[String], attempts: Int): Unit = {
    println(s"Attempt $attempts at $path")
    val messages = ListBuffer.empty[String]
    try {
      val url = new java.net.URL("http://localhost:9000" + path)
      val conn = url.openConnection().asInstanceOf[java.net.HttpURLConnection]
      conn.setConnectTimeout(ConnectTimeout)
      conn.setReadTimeout(ReadTimeout)      

      if (status == conn.getResponseCode) {
        messages += s"Resource at $path returned $status as expected"
      } else {
        throw new RuntimeException(s"Resource at $path returned ${conn.getResponseCode} instead of $status")
      }

      val is = if (conn.getResponseCode >= 400) {
        conn.getErrorStream
      } else {
        conn.getInputStream
      }

      // The input stream may be null if there's no body
      val contents = if (is != null) {
        val c = IO.readStream(is)
        is.close()
        c
      } else ""
      conn.disconnect()

      assertions.foreach { assertion =>
        if (contents.contains(assertion)) {
          messages += s"Resource at $path contained $assertion"
        } else {
          throw new RuntimeException(s"Resource at $path didn't contain '$assertion':\n$contents")
        }
      }

      messages.foreach(println)
    } catch {
      case e: Exception =>
        println(s"Got exception: $e")
        if (attempts < MaxAttempts) {
          Thread.sleep(WaitTime)
          verifyResourceContains(path, status, assertions, attempts + 1)
        } else {
          messages.foreach(println)
          println(s"After $attempts attempts:")
          throw e
        }
    }
  }
}
