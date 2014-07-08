lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.10.4")

PlayKeys.playInteractionMode := play.StaticPlayNonBlockingInteractionMode

InputKey[Unit]("verify-reloads") := {
  val expected = Def.spaceDelimited().parsed.head.toInt
  val actual = IO.readLines(target.value / "reload.log").count(_.nonEmpty)
  if (expected == actual) {
    println(s"Expected and got $expected reloads")
  } else {
    throw new RuntimeException(s"Expected $expected reloads but got $actual")
  }
}

InputKey[Unit]("verify-resource-contains") := {
  val args = Def.spaceDelimited("<path> <status> <words> ...").parsed
  val path = args.head
  val status = args.tail.head.toInt
  val assertions = args.tail.tail
  val url = new java.net.URL("http://localhost:9000" + path)
  val conn = url.openConnection().asInstanceOf[java.net.HttpURLConnection]
  if (status == conn.getResponseCode) {
    println(s"Resource at $path returned $status as expected")
  } else {
    throw new RuntimeException(s"Resource at $path returned ${conn.getResponseCode} instead of $status")
  }
  val is = if (conn.getResponseCode >= 400) {
    conn.getErrorStream
  } else {
    conn.getInputStream
  }
  val contents = IO.readStream(is)
  is.close()
  conn.disconnect()
  assertions.foreach { assertion =>
    if (contents.contains(assertion)) {
      println(s"Resource at $path contained $assertion")
    } else {
      throw new RuntimeException(s"Resource at $path didn't contain '$assertion':\n$contents")
    }
  }
}