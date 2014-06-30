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
  val args = Def.spaceDelimited("<path> <words> ...").parsed
  val path = args.head
  val assertions = args.tail
  val url = new java.net.URL("http://localhost:9000" + path)
  val is = url.openStream()
  val contents = IO.readStream(is)
  is.close()
  assertions.foreach { assertion =>
    if (contents.contains(assertion)) {
      println(s"Resource at $path contained $assertion")
    } else {
      throw new RuntimeException(s"Resource at $path didn't contain '$assertion':\n$contents")
    }
  }
}