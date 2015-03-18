/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http.parsing

import play.api.libs.Files.TemporaryFile
import play.api.libs.iteratee.Enumerator
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
        parts.dataParts.get("text1") must beSome.like {
          case field :: Nil => field must_== "the first text field"
        }
        parts.dataParts.get("text2:colon") must beSome.like {
          case field :: Nil => field must_== "the second text field"
        }
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

      val result = await(Enumerator(body.getBytes("utf-8")).run(parser))

      checkResult(result)
    }

    "validate the full length of the body" in new WithApplication(FakeApplication(
      additionalConfiguration = Map("play.http.parser.maxDiskBuffer" -> "100")
    )) {
      val parser = parse.multipartFormData.apply(FakeRequest().withHeaders(
        CONTENT_TYPE -> "multipart/form-data; boundary=aabbccddee"
      ))

      val result = await(Enumerator(body.getBytes("utf-8")).run(parser))

      result must beLeft.like {
        case error => error.header.status must_== REQUEST_ENTITY_TOO_LARGE
      }
    }

    "not parse more than the max data length" in new WithApplication(FakeApplication(
      additionalConfiguration = Map("play.http.parser.maxMemoryBuffer" -> "30")
    )) {
      val parser = parse.multipartFormData.apply(FakeRequest().withHeaders(
        CONTENT_TYPE -> "multipart/form-data; boundary=aabbccddee"
      ))

      val result = await(Enumerator(body.getBytes("utf-8")).run(parser))

      result must beLeft.like {
        case error => error.header.status must_== REQUEST_ENTITY_TOO_LARGE
      }
    }

    "work if there's no crlf at the start" in new WithApplication() {
      val parser = parse.multipartFormData.apply(FakeRequest().withHeaders(
        CONTENT_TYPE -> "multipart/form-data; boundary=aabbccddee"
      ))

      val result = await(Enumerator(body.trim.getBytes("utf-8")).run(parser))

      checkResult(result)
    }

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

}
