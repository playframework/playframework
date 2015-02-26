/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.forkrun.protocol

import org.specs2.mutable._
import play.forkrun.protocol.Serializers._
import play.runsupport.Reloader.{ Source, CompileSuccess, CompileFailure, CompileResult }
import sbt.serialization._

object SerializersSpec extends Specification with PicklingTestUtils {

  def file(path: String): java.io.File = {
    (new java.io.File(path)).getAbsoluteFile
  }

  "serializers" should {

    "roundtrip tuple2" in {
      roundTrip("a" -> 42)
    }

    "roundtrip watch services" in {
      roundTrip(ForkConfig.DefaultWatchService)
      roundTrip(ForkConfig.JDK7WatchService)
      roundTrip(ForkConfig.JNotifyWatchService)
    }

    "roundtrip source file" in {
      roundTrip(Source(file("foo"), Some(file("bar"))))
    }

    "roundtrip source map" in {
      roundTrip(Map("foo" -> Source(file("foo"), Some(file("bar")))))
    }

    "roundtrip play exception" in {
      roundTrip(new play.api.PlayException("foo", "bar"))
    }

    "roundtrip compile success" in {
      val sourceMap = Map("foo" -> Source(file("foo"), Some(file("bar"))))
      val classpath = Seq(file("baz"))
      roundTrip(CompileSuccess(sourceMap, classpath))
    }

    "roundtrip play server started" in {
      roundTrip(PlayServerStarted("http://localhost:9000"))
    }

    "roundtrip fork config" in {
      val forkConfig = ForkConfig(
        projectDirectory = file("foo"),
        javaOptions = Seq("a"),
        dependencyClasspath = Seq(file("bar")),
        allAssets = Seq("a" -> file("baz")),
        docsClasspath = Seq(file("blah")),
        docsJar = Option(file("docs")),
        devSettings = Seq("a" -> "b"),
        defaultHttpPort = 3456,
        defaultHttpAddress = "1.2.3.4",
        watchService = ForkConfig.JNotifyWatchService,
        monitoredFiles = Seq("c"),
        targetDirectory = file("target"),
        pollInterval = 100,
        notifyKey = "abcdefg",
        reloadKey = "hijklmnop",
        compileTimeout = 1000,
        mainClass = "play.Server"
      )
      roundTrip(forkConfig)
    }
  }
}

trait PicklingTestUtils extends Specification {

  import org.specs2.matcher._

  private def addWhatWeWerePickling[T, U](t: T)(body: => U): U =
    try body
    catch {
      case e: Throwable =>
        e.printStackTrace()
        throw new AssertionError(s"Crash round-tripping ${t.getClass.getName}: value was: ${t}", e)
    }

  def roundTripArray[A](x: Array[A])(implicit ev0: Pickler[Array[A]], ev1: Unpickler[Array[A]]): MatchResult[Any] =
    roundTripBase[Array[A]](x)((a, b) =>
      (a.toList) must beEqualTo(b.toList)) { (a, b) =>
      a.getMessage must beEqualTo(b.getMessage)
    }

  def roundTrip[A: Pickler: Unpickler](x: A): MatchResult[Any] =
    roundTripBase[A](x)((a, b) =>
      a must beEqualTo(b)) { (a, b) =>
      a.getMessage must beEqualTo(b.getMessage)
    }

  def roundTripBase[A: Pickler: Unpickler](a: A)(f: (A, A) => MatchResult[Any])(e: (Throwable, Throwable) => MatchResult[Any]): MatchResult[Any] = addWhatWeWerePickling(a) {
    val json = SerializedValue(a).toJsonString
    //System.err.println(s"json: $json")
    val parsed = SerializedValue.fromJsonString(json).parse[A].get
    (a, parsed) match {
      case (a: Throwable, parsed: Throwable) => e(a, parsed)
      case _ => f(a, parsed)
    }
  }

}
