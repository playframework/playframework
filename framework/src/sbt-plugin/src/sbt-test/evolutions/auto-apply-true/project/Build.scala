/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

object DevModeBuild {

  // Using 30 max attempts so that we can give more chances to
  // the file watcher service. This is relevant when using the
  // default JDK watch service which does uses polling.
  val MaxAttempts = 30
  val WaitTime = 500l

  val ConnectTimeout = 10000
  val ReadTimeout = 10000

  def applyEvolutions(path: String): Unit = callUrl(path)

  @tailrec
  def verifyResourceContains(path: String, status: Int, assertions: Seq[String], attempts: Int, headers: (String, String)*): Unit = {
    println(s"Attempt $attempts at $path")
    val messages = ListBuffer.empty[String]
    try {
      val (requestStatus, contents) = callUrl(path, headers: _*)

      if (status == requestStatus) {
        messages += s"Resource at $path returned $status as expected"
      } else {
        throw new RuntimeException(s"Resource at $path returned ${requestStatus} instead of $status")
      }

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
          verifyResourceContains(path, status, assertions, attempts + 1, headers: _*)
        } else {
          messages.foreach(println)
          println(s"After $attempts attempts:")
          throw e
        }
    }
  }

  private def callUrl(path: String, headers: (String, String)*): (Int, String) = {
    val url = new java.net.URL("http://localhost:9000" + path)
    val conn = url.openConnection().asInstanceOf[java.net.HttpURLConnection]
    conn.setConnectTimeout(ConnectTimeout)
    conn.setReadTimeout(ReadTimeout)

    headers.foreach(h => conn.setRequestProperty(h._1, h._2))

    val status = conn.getResponseCode

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

    (status, contents)
  }
}
