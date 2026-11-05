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

object ContinuousRunTest {
  private var step          = 0
  private var libraryBytes  = Array.emptyByteArray
  private var argumentsPort = 0

  def freePort(): Int = {
    val socket = new ServerSocket(0)
    try socket.getLocalPort
    finally socket.close()
  }

  def record(base: File, name: String): Unit = IO.append(base / "target" / s"$name.log", "event\n")

  def count(base: File, name: String): Int = {
    val log = base / "target" / s"$name.log"
    if (log.exists) IO.readLines(log).size else 0
  }

  private def request(port: Int, path: String, status: Int, expected: String*): Unit = {
    val connection = URI.create(s"http://127.0.0.1:$port$path").toURL().openConnection().asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(10000)
    connection.setReadTimeout(30000)
    try {
      assert(connection.getResponseCode == status, s"Unexpected response for $path: ${connection.getResponseCode}")
      val stream = if (status >= 400) connection.getErrorStream else connection.getInputStream
      val source = scala.io.Source.fromInputStream(stream)
      val body   =
        try source.mkString
        finally source.close()
      expected.foreach(word => assert(body.contains(word), s"Response for $path did not contain '$word': $body"))
    } finally connection.disconnect()
  }

  def onIteration(base: File, classes: File, libraryClasses: File, port: Int): Watch.Action =
    try checkIteration(base, classes, libraryClasses, port)
    catch {
      case scala.util.control.NonFatal(error) => new Watch.HandleError(error)
    }

  private def checkIteration(base: File, classes: File, libraryClasses: File, port: Int): Watch.Action = {
    val controller    = base / "app" / "controllers" / "HomeController.scala"
    val greeting      = base / "library" / "src" / "main" / "scala" / "example" / "Greeting.scala"
    val template      = base / "app" / "views" / "index.scala.html"
    val newSource     = base / "new-sources" / "Created.scala"
    val newTemplate   = base / "app" / "views" / "nested" / "extra.scala.html"
    val config        = base / "conf" / "application.conf"
    val routes        = base / "conf" / "routes"
    val greetingClass = libraryClasses / "example" / "Greeting$.class"
    println(s"Checking continuous run step $step")
    // Keep distinct edits outside sbt's default 500 ms anti-entropy window.
    Thread.sleep(1000)
    step match {
      case 0 =>
        assert((classes / "controllers" / "HomeController.class").exists, "Initial compilation did not run")
        assert(count(base, "application-starts") == 0, "Compilation eagerly loaded the application")
        libraryBytes = IO.readBytes(greetingClass)
        IO.write(greeting, IO.read(greeting).replace("original-library", "changed-library"))
      case 1 =>
        assert(!java.util.Arrays.equals(libraryBytes, IO.readBytes(greetingClass)), "Dependency was not recompiled")
        assert(count(base, "application-starts") == 0, "Compilation reloaded the application before a request")
        request(port, "/", 200, "changed-library", "original-template", "original-config")
        assert(count(base, "application-starts") == 1)
        IO.write(template, IO.read(template).replace("original-template", "changed-template"))
      case 2 =>
        assert(count(base, "application-starts") == 1)
        request(port, "/", 200, "changed-template")
        assert(count(base, "application-starts") == 2)
        IO.write(newSource, "class Created\n")
        IO.write(newTemplate, "@()\nnew-template\n")
      case 3 =>
        assert((classes / "Created.class").exists, "Source in a newly created directory was not compiled")
        assert((classes / "views" / "html" / "nested" / "extra.class").exists, "New template was not compiled")
        IO.delete(newSource)
        IO.delete(newTemplate)
      case 4 =>
        assert(!(classes / "Created.class").exists, "Deleted source's class remains")
        assert(!(classes / "views" / "html" / "nested" / "extra.class").exists, "Deleted template's class remains")
        IO.write(controller, IO.read(controller).replace("Ok(", "doesNotExist("))
      case 5 =>
        request(port, "/", 500, "doesNotExist")
        IO.write(controller, IO.read(controller).replace("doesNotExist(", "Ok("))
      case 6 =>
        request(port, "/", 200, "changed-library", "changed-template")
        assert(count(base, "application-starts") == 3)
        IO.write(config, IO.read(config).replace("original-config", "changed-config"))
      case 7 =>
        request(port, "/", 200, "changed-config")
        assert(count(base, "application-starts") == 4)
        IO.append(routes, "\nGET /other controllers.HomeController.index\n")
      case 8 =>
        request(port, "/other", 200, "changed-config")
        assert(count(base, "application-starts") == 5)
        IO.write(base / "public" / "plain.txt", "changed-asset\n")
      case 9 =>
        request(port, "/assets/plain.txt", 200, "changed-asset")
        assert(count(base, "application-starts") == 5, "Static assets reloaded the application")
        assert(count(base, "server-starts") == 1, "The server was restarted between iterations")
        assert(count(base, "server-stops") == 0)
      case _ => sys.error(s"Unexpected iteration $step")
    }
    step += 1
    if (step == 10) Watch.CancelWatch else Watch.Ignore
  }

  def verifyPortClosed(port: Int): Unit = {
    val socket = new Socket()
    try {
      try {
        socket.connect(new InetSocketAddress("127.0.0.1", port), 1000)
        sys.error(s"Server is still listening on port $port")
      } catch {
        case _: java.net.ConnectException => ()
      }
    } finally socket.close()
  }

  def runWithArguments(state: State): State = {
    argumentsPort = freePort()
    s"~Compile/run -Dhttp.port=$argumentsPort -Dhttp.address=127.0.0.1" :: state
  }

  def verifyArguments(base: File): Unit = {
    assert(count(base, "server-starts") == 2)
    assert(count(base, "server-stops") == 2)
    assert(count(base, "watch-stops") == 2)
    verifyPortClosed(argumentsPort)
    assert(System.getProperty("http.port") == null)
  }

  def verifyStopped(base: File, port: Int): Unit = {
    assert(step == 10, s"Only $step iterations completed")
    assert(count(base, "server-stops") == 1, "Server did not stop with the watch")
    assert(count(base, "watch-stops") == 1, "User's termination callback was not called")
    assert(count(base, "application-stops") == count(base, "application-starts"), "Application stop hooks did not run")
    verifyPortClosed(port)
  }

  def reset(base: File): Unit = {
    step = 0
    // sbt 2 cleans its configuration-specific output directories, so clear these logs explicitly.
    Seq("application-starts", "application-stops", "server-starts", "server-stops", "watch-stops")
      .foreach(name => IO.delete(base / "target" / s"$name.log"))
    for (
      file <- Seq(
        base / "library" / "src" / "main" / "scala" / "example" / "Greeting.scala",
        base / "app" / "views" / "index.scala.html",
        base / "conf" / "application.conf"
      )
    ) IO.write(file, IO.read(file).replace("changed-", "original-"))
    val routes = base / "conf" / "routes"
    IO.write(routes, IO.read(routes).replace("\nGET /other controllers.HomeController.index\n", ""))
  }
}
