/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI

import sbt._
import sbt.nio.Watch

object ContinuousRunLifecycleTest {
  private var step = 0
  private var libraryBytes = Array.emptyByteArray
  private var initialWarnings = 0

  def freePort(): Int = {
    val socket = new ServerSocket(0)
    try socket.getLocalPort
    finally socket.close()
  }

  def record(base: File, name: String): Unit = IO.append(base / "target" / s"$name.log", "event\n")

  def count(base: File, name: String): Int = {
    val file = base / "target" / s"$name.log"
    if (file.exists) IO.readLines(file).size else 0
  }

  private def warnings(base: File): Int = {
    val file = base / "target" / "lifecycle-output.log"
    if (file.exists) IO.readLines(file).count(_.contains("The previous continuous-run server")) else 0
  }

  def request(port: Int): Unit = {
    val connection = URI.create(s"http://127.0.0.1:$port/").toURL().openConnection().asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(10000)
    connection.setReadTimeout(30000)
    try assert(connection.getResponseCode == 200, "The restarted server could not serve the application")
    finally connection.disconnect()
  }

  def verify(base: File, port: Int, expected: Seq[Int]): Unit = {
    assert(expected.size == 3)
    assert(count(base, "server-starts") == expected(0), "Unexpected server starts")
    assert(count(base, "server-stops") == expected(1), "Unexpected server stops")
    assert(warnings(base) - initialWarnings == expected(2), "Unexpected recovery warnings")
    assert(count(base, "user-callbacks") >= 1, "The user's termination callback was lost")
    if (expected(0) == expected(1)) {
      val socket = new Socket()
      try {
        try {
          socket.connect(new InetSocketAddress("127.0.0.1", port), 1000)
          sys.error("The development server still holds its port")
        } catch {
          case _: java.net.ConnectException => ()
        }
      } finally socket.close()
    }
  }

  def onIteration(base: File, classes: File, port: Int): Watch.Action = {
    try {
      val source = base / "library" / "src" / "main" / "scala" / "example" / "Greeting.scala"
      val product = classes / "example" / "Greeting$.class"
      println(s"Checking lifecycle watch step $step")
      step match {
        case 0 =>
          assert(count(base, "server-starts") == 1)
        case 1 =>
          assert(count(base, "server-starts") == 2, "The second watch reused the previous watch's server")
          libraryBytes = IO.readBytes(product)
          Thread.sleep(1000)
          IO.write(source, IO.read(source).replace("original-library", "changed-library"))
        case 2 =>
          assert(!java.util.Arrays.equals(libraryBytes, IO.readBytes(product)), "The second watch missed a dependency change")
          request(port)
        case _ => sys.error(s"Unexpected lifecycle step $step")
      }
      step += 1
      if (step == 2) Watch.Ignore else Watch.CancelWatch
    } catch {
      case scala.util.control.NonFatal(error) => new Watch.HandleError(error)
    }
  }

  def reset(base: File): Unit = {
    step = 0
    initialWarnings = warnings(base)
    Seq("server-starts", "server-stops", "user-callbacks", "wrapped-callbacks")
      .foreach(name => IO.delete(base / "target" / s"$name.log"))
    val source = base / "library" / "src" / "main" / "scala" / "example" / "Greeting.scala"
    IO.write(source, IO.read(source).replace("changed-library", "original-library"))
  }
}
