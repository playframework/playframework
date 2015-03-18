/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.gzip

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

import org.apache.commons.io.IOUtils
import org.specs2.time.NoTimeConversions

import concurrent.Await
import play.api.libs.iteratee.{ Iteratee, Enumeratee, Enumerator }
import concurrent.duration._
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext.Implicits.global

object GzipSpec extends Specification with NoTimeConversions {

  "gzip" should {

    /**
     * Uses both Java's GZIPOutputStream and GZIPInputStream to verify correctness.
     */
    def test(values: String*) = {
      import java.io._
      import java.util.zip._

      val valuesBytes = values.map(_.getBytes("utf-8"))

      val result: Array[Byte] = Await.result(Enumerator.enumerate(valuesBytes) &> Gzip.gzip() |>>> Iteratee.consume[Array[Byte]](), Duration.Inf)

      // Check that it exactly matches the gzip output stream
      val baos = new ByteArrayOutputStream()
      val os = new GZIPOutputStream(baos)
      valuesBytes.foreach(bytes => os.write(bytes))
      os.close()
      val baosResult = baos.toByteArray

      for (i <- 0 until result.length) {
        if (result(i) != baosResult(i)) {
          result(i) must_== baosResult(i)
        }
      }

      result must_== baos.toByteArray

      // Check that it can be unzipped
      val bais = new ByteArrayInputStream(result)
      val is = new GZIPInputStream(bais)
      val check: Array[Byte] = Await.result(Enumerator.fromStream(is) |>>> Iteratee.consume[Array[Byte]](), 10.seconds)
      values.mkString("") must_== new String(check, "utf-8")
    }

    "gzip simple input" in {
      test("Hello world")
    }

    "gzip multiple inputs" in {
      test("Hello", " ", "world")
    }

    "gzip large repeating input" in {
      val bigString = Seq.fill(1000)("Hello world").mkString("")
      test(bigString)
    }

    "gzip multiple large repeating inputs" in {
      val bigString = Seq.fill(1000)("Hello world").mkString("")
      test(bigString, bigString, bigString)
    }

    "gzip large random input" in {
      test(scala.util.Random.nextString(10000))
    }

    "gzip multiple large random inputs" in {
      test(scala.util.Random.nextString(10000),
        scala.util.Random.nextString(10000),
        scala.util.Random.nextString(10000))
    }
  }

  "gunzip" should {

    def gzip(value: String): Array[Byte] = {
      val baos = new ByteArrayOutputStream()
      val gzipStream = new GZIPOutputStream(baos)
      gzipStream.write(value.getBytes("utf-8"))
      gzipStream.close()
      baos.toByteArray
    }

    def read(resource: String): Array[Byte] = {
      val is = GzipSpec.getClass.getClassLoader.getResourceAsStream(resource)
      try {
        IOUtils.toByteArray(is)
      } finally {
        is.close()
      }
    }

    def test(value: String, gunzip: Enumeratee[Array[Byte], Array[Byte]] = Gzip.gunzip(), chunkSize: Option[Int] = None) = {
      testInput(gzip(value), value, gunzip, chunkSize)
    }

    def testInput(input: Array[Byte], expected: String, gunzip: Enumeratee[Array[Byte], Array[Byte]] = Gzip.gunzip(), chunkSize: Option[Int] = None) = {
      val gzipEnumerator = chunkSize match {
        case Some(size) => Enumerator.enumerate(input.grouped(size))
        case None => Enumerator(input)
      }
      val future = gzipEnumerator &> gunzip |>>> Iteratee.consume[Array[Byte]]()
      val result = new String(Await.result(future, 10.seconds), "utf-8")
      result must_== expected
    }

    "gunzip simple input" in {
      test("Hello world")
    }

    "gunzip simple input in small chunks" in {
      test("Hello world", chunkSize = Some(5))
    }

    "gunzip simple input in individual bytes" in {
      test("Hello world", chunkSize = Some(1))
    }

    "gunzip large repeating input" in {
      test(Seq.fill(1000)("Hello world").mkString(""))
    }

    "gunzip large repeating input in small chunks" in {
      test(Seq.fill(1000)("Hello world").mkString(""), chunkSize = Some(100))
    }

    "gunzip large random input" in {
      test(scala.util.Random.nextString(10000))
    }

    "gunzip large random input in small chunks" in {
      test(scala.util.Random.nextString(10000), chunkSize = Some(100))
    }

    "gunzip a stream with a filename" in {
      testInput(read("helloWorld.txt.gz"), "Hello world")
    }

    "gunzip a stream with a filename in individual bytes" in {
      testInput(read("helloWorld.txt.gz"), "Hello world", chunkSize = Some(1))
    }

  }
}
