/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._

object DevModeBuild {

  val ConnectTimeout = 10000
  val ReadTimeout    = 10000

  def callIndex(): Unit = callUrl("/")

  private def callUrl(path: String, headers: (String, String)*): (Int, String) = {
    val url  = new java.net.URL("http://localhost:9000" + path)
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
