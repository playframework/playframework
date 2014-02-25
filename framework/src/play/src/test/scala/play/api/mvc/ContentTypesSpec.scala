/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import org.specs2.mutable.Specification
import scalax.io.Resource
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.libs.iteratee.Enumerator

object ContentTypesSpec extends Specification {

  import play.api.mvc.BodyParsers.parse.Multipart._

  "FileInfoMatcher" should {

    "parse headers with semicolon inside quotes" in {
      val result = FileInfoMatcher.unapply(Map("content-disposition" -> """form-data; name="document"; filename="semicolon;inside.jpg"""", "content-type" -> "image/jpeg"))
      result must not beEmpty;
      result.get must equalTo(("document", "semicolon;inside.jpg", Option("image/jpeg")));
    }
    
    "parse headers with escaped quote inside quotes" in {
      val result = FileInfoMatcher.unapply(Map("content-disposition" -> """form-data; name="document"; filename="quotes\"\".jpg"""", "content-type" -> "image/jpeg"))
      result must not beEmpty;
      result.get must equalTo(("document", """quotes"".jpg""", Option("image/jpeg")));
    }

  }

  "RawBuffer" should {
    implicit def stringToBytes(s: String): Array[Byte] = s.getBytes("utf-8")

    "work in memory" in {
      val buffer = RawBuffer(100)
      buffer.size must_== 0
      buffer.push("hello")
      buffer.push(" ")
      buffer.push("world")
      buffer.size must_== 11
      buffer.asBytes() must beSome.like {
        case bytes => new String(bytes) must_== "hello world"
      }
    }

    "write out to a file" in {
      val buffer = RawBuffer(10)
      buffer.push("hello")
      buffer.push(" ")
      buffer.push("world")
      buffer.size must_== 11
      buffer.close()
      buffer.asBytes() must beNone
      buffer.asBytes(11) must beSome.like {
        case bytes => new String(bytes) must_== "hello world"
      }
    }

    def rand(size: Int) = {
      new String(scala.util.Random.alphanumeric.take(size).toArray[Char])
    }

    "extend the size by a small amount" in {
      val buffer = RawBuffer(1024 * 100)
      // RawBuffer starts with 8192 buffer size, write 8000 bytes, then another 400, make sure that works
      val big = rand(8000)
      val small = rand(400)
      buffer.push(big)
      buffer.push(small)
      buffer.size must_== 8400
      buffer.asBytes() must beSome.like {
        case bytes => new String(bytes) must_== (big + small)
      }
    }

    "extend the size by a large amount" in {
      val buffer = RawBuffer(1024 * 100)
      // RawBuffer starts with 8192 buffer size, write 8000 bytes, then another 8000, make sure that works
      val big = rand(8000)
      buffer.push(big)
      buffer.push(big)
      buffer.size must_== 16000
      buffer.asBytes() must beSome.like {
        case bytes => new String(bytes) must_== (big + big)
      }
    }

    "allow something that fits in memory to be accessed as a file" in {
      val buffer = RawBuffer(20)
      buffer.push("hello")
      buffer.push(" ")
      buffer.push("world")
      buffer.size must_== 11
      val file = buffer.asFile
      Resource.fromFile(file).string must_== "hello world"
    }
  }

  "Multipart parser" should {
    
    "get the file parts" in {
      testMultiPart("-----------------------------117723558510316372842092349957\r\nContent-Disposition: form-data; name=\"picture\"; filename=\"README\"\r\nContent-Type: application/octet-stream\r\n\r\nThis is your new Play application\r\n=====================================\r\nThis file will be packaged with your application, when using `play dist`.\r\n-----------------------------117723558510316372842092349957--\r\n")
    }

    "get the file parts with boundary that has no CRLF at start"  in {
      testMultiPart("-----------------------------117723558510316372842092349957Content-Disposition: form-data; name=\"picture\"; filename=\"README\"\r\nContent-Type: application/octet-stream\r\n\r\nThis is your new Play application\r\n=====================================\r\nThis file will be packaged with your application, when using `play dist`.\r\n-----------------------------117723558510316372842092349957--\r\n")
    }

    def testMultiPart(testMultipartBody: String) = {

      def await[T](f: Future[T]) = Await.result(f, Duration("5 seconds"))
    	case class TestRequestHeader(headers: Headers, method: String = "GET", uri: String = "/", path: String = "", remoteAddress: String = "127.0.0.1", version: String = "HTTP/1.1", id: Long = 1, tags: Map[String, String] = Map.empty[String, String], queryString: Map[String, Seq[String]] = Map(), secure: Boolean = false) extends RequestHeader
      val multipartFormDataParser = BodyParsers.parse.multipartFormData

      val rh = TestRequestHeader(headers = new Headers {
        val data = Seq(play.api.http.HeaderNames.CONTENT_TYPE -> Seq("multipart/form-data; boundary=---------------------------117723558510316372842092349957"), play.api.http.HeaderNames.CONTENT_LENGTH -> Seq("382"))
      })

      val body = Enumerator(testMultipartBody.getBytes)
      val parsedResult = await(body run multipartFormDataParser(rh))

      parsedResult.isRight must beTrue

      parsedResult match {
        case Right(multipartFormData) =>
          multipartFormData.badParts.isEmpty must beTrue
          multipartFormData.missingFileParts.isEmpty must beTrue
          multipartFormData.dataParts.isEmpty must beTrue

          multipartFormData.files.size must_== 1
          val filePart = multipartFormData.files.head
          filePart.filename must_== "README"
          filePart.contentType must beSome("application/octet-stream")
          filePart.key must_== "picture"
          success
        case Left(_) =>
          failure("must not get a Left result")
      }
    }
  }
}

