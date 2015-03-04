/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.util.Properties

/**
 * The source code for this object has been purloined from one of the
 * SBT Plugin's dev mode tests.
 */
object DevModeBuild {

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
