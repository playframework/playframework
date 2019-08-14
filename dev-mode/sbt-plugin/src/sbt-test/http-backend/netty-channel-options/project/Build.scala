/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._

object DevModeBuild {
  def callIndex(): Unit = callUrl("/")

  def checkLines(source: String, target: String): Unit = {
    val sourceLines = IO.readLines(new File(source))
    val targetLines = IO.readLines(new File(target))

    println("Source:")
    println("-------")
    println(sourceLines.mkString("\n"))
    println("Target:")
    println("-------")
    println(targetLines.mkString("\n"))

    sourceLines.foreach { sl =>
      if (!targetLines.contains(sl)) {
        throw new RuntimeException(s"File $target didn't contain line:\n$sl")
      }
    }
  }

  private def callUrl(path: String, headers: (String, String)*): (Int, String) = {
    val url  = new java.net.URL(s"http://localhost:9000$path")
    val conn = url.openConnection().asInstanceOf[java.net.HttpURLConnection]
    conn.setConnectTimeout(10000)
    conn.setReadTimeout(10000)
    headers.foreach { case (k, v) => conn.setRequestProperty(k, v) }
    try {
      val status   = conn.getResponseCode
      val in       = if (status < 400) conn.getInputStream else conn.getErrorStream
      val contents = if (in == null) "" else try IO.readStream(in) finally in.close()
      (status, contents)
    } finally conn.disconnect()
  }
}
