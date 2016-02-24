/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http.parsing

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{ Result, MultipartFormData, BodyParsers }
import play.api.test._
import play.core.parsers.Multipart.FileInfoMatcher
import play.utils.PlayIO

object MultipartFormDataParserSpec extends PlaySpecification {

  val body =
    """
      |--aabbccddee
      |Content-Disposition: form-data; name="text1"
      |
      |the first text field
      |--aabbccddee
      |Content-Disposition: form-data; name="text2:colon"
      |
      |the second text field
      |--aabbccddee
      |Content-Disposition: form-data; name="file1"; filename="file1.txt"
      |Content-Type: text/plain
      |
      |the first file
      |
      |--aabbccddee
      |Content-Disposition: form-data; name="file2"; filename="file2.txt"
      |Content-Type: text/plain
      |
      |the second file
      |
      |--aabbccddee--
      |""".stripMargin.lines.mkString("\r\n")

  val parse = new BodyParsers() {}.parse

  def checkResult(result: Either[Result, MultipartFormData[TemporaryFile]]) = {
    result must beRight.like {
      case parts =>
        parts.dataParts.get("text1") must_== Some(Seq("the first text field"))
        parts.dataParts.get("text2:colon") must_== Some(Seq("the second text field"))
        parts.files must haveLength(2)
        parts.file("file1") must beSome.like {
          case filePart => PlayIO.readFileAsString(filePart.ref.file) must_== "the first file\r\n"
        }
        parts.file("file2") must beSome.like {
          case filePart => PlayIO.readFileAsString(filePart.ref.file) must_== "the second file\r\n"
        }
    }
  }

  "The multipart/form-data parser" should {
    "parse some content" in new WithApplication() {
      val parser = parse.multipartFormData.apply(FakeRequest().withHeaders(
        CONTENT_TYPE -> "multipart/form-data; boundary=aabbccddee"
      ))

      val result = await(parser.run(Source.single(ByteString(body))))

      checkResult(result)
    }

    "parse some content that arrives one byte at a time" in new WithApplication() {
      val parser = parse.multipartFormData.apply(FakeRequest().withHeaders(
        CONTENT_TYPE -> "multipart/form-data; boundary=aabbccddee"
      ))

      val bytes = body.getBytes.map(byte => ByteString(byte)).toVector
      val result = await(parser.run(Source(bytes)))

      checkResult(result)
    }

    "return bad request for invalid body" in new WithApplication() {
      val parser = parse.multipartFormData.apply(FakeRequest().withHeaders(
        CONTENT_TYPE -> "multipart/form-data" // no boundary
      ))

      val result = await(parser.run(Source.single(ByteString(body))))

      result must beLeft.like {
        case error => error.header.status must_== BAD_REQUEST
      }
    }

    "validate the full length of the body" in new WithApplication(
      _.configure("play.http.parser.maxDiskBuffer" -> "100")
    ) {
      val parser = parse.multipartFormData.apply(FakeRequest().withHeaders(
        CONTENT_TYPE -> "multipart/form-data; boundary=aabbccddee"
      ))

      val result = await(parser.run(Source.single(ByteString(body))))

      result must beLeft.like {
        case error => error.header.status must_== REQUEST_ENTITY_TOO_LARGE
      }
    }

    "not parse more than the max data length" in new WithApplication(
      _.configure("play.http.parser.maxMemoryBuffer" -> "30")
    ) {
      val parser = parse.multipartFormData.apply(FakeRequest().withHeaders(
        CONTENT_TYPE -> "multipart/form-data; boundary=aabbccddee"
      ))

      val result = await(parser.run(Source.single(ByteString(body))))

      result must beLeft.like {
        case error => error.header.status must_== REQUEST_ENTITY_TOO_LARGE
      }
    }

    "work if there's no crlf at the start" in new WithApplication() {
      val parser = parse.multipartFormData.apply(FakeRequest().withHeaders(
        CONTENT_TYPE -> "multipart/form-data; boundary=aabbccddee"
      ))

      val result = await(parser.run(Source.single(ByteString(body))))

      checkResult(result)
    }

    "parse headers with semicolon inside quotes" in {
      val result = FileInfoMatcher.unapply(Map("content-disposition" -> """form-data; name="document"; filename="semicolon;inside.jpg"""", "content-type" -> "image/jpeg"))
      result must not(beEmpty)
      result.get must equalTo(("document", "semicolon;inside.jpg", Option("image/jpeg")))
    }

    "parse headers with escaped quote inside quotes" in {
      val result = FileInfoMatcher.unapply(Map("content-disposition" -> """form-data; name="document"; filename="quotes\"\".jpg"""", "content-type" -> "image/jpeg"))
      result must not(beEmpty)
      result.get must equalTo(("document", """quotes"".jpg""", Option("image/jpeg")))
    }

    "parse unquoted content disposition" in {
      val result = FileInfoMatcher.unapply(Map("content-disposition" -> """form-data; name=document; filename=hello.txt"""))
      result must not(beEmpty)
      result.get must equalTo(("document", "hello.txt", None))
    }

    "ignore extended filename in content disposition" in {
      val result = FileInfoMatcher.unapply(Map("content-disposition" -> """form-data; name=document; filename=hello.txt; filename*=utf-8''ignored.txt"""))
      result must not(beEmpty)
      result.get must equalTo(("document", "hello.txt", None))
    }
  }

}
