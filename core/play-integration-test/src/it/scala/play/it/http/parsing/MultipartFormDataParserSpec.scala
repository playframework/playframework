/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.parsing

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.Application
import play.api.BuiltInComponentsFromContext
import play.api.NoHttpFiltersComponents
import play.api.libs.Files.TemporaryFile
import play.api.libs.Files.TemporaryFileCreator
import play.api.mvc._
import play.api.test._
import play.core.parsers.Multipart.FileInfoMatcher
import play.core.parsers.Multipart.PartInfoMatcher
import play.utils.PlayIO
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData.BadPart
import play.api.mvc.MultipartFormData.FilePart
import play.api.routing.Router
import play.core.server.Server

class MultipartFormDataParserSpec extends PlaySpecification with WsTestClient {
  sequential

  // To make the test clear and also avoid code editors to trim
  // empty spaces here.s
  val emptySpace = " "

  val body =
    s"""
       |--aabbccddee
       |Content-Disposition: form-data; name="text1"
       |
       |the first text field
       |--aabbccddee
       |Content-Disposition: form-data; name="text2:colon"
       |
       |the second text field
       |--aabbccddee
       |Content-Disposition: form-data; name=noQuotesText1
       |
       |text field with unquoted name
       |--aabbccddee
       |Content-Disposition: form-data; name=noQuotesText1:colon
       |
       |text field with unquoted name and colon
       |--aabbccddee
       |Content-Disposition: form-data; name="empty_text"
       |
       |
       |--aabbccddee
       |Content-Disposition: form-data; name=""
       |
       |empty name should work
       |--aabbccddee
       |Content-Disposition: form-data; name="arr[]"
       |
       |array value 0
       |--aabbccddee
       |Content-Disposition: form-data; name="arr[]"
       |
       |array value 1
       |--aabbccddee
       |Content-Disposition: form-data; name="orderedarr[0]"
       |
       |ordered array value 0
       |--aabbccddee
       |Content-Disposition: form-data; name="orderedarr[1]"
       |
       |ordered array value 1
       |--aabbccddee
       |Content-Disposition: form-data; name="file_with_space_only"; filename="with_space_only.txt"
       |Content-Type: text/plain
       |
       |${emptySpace}
       |--aabbccddee
       |Content-Disposition: form-data; name="file_with_newline_only"; filename="with_newline_only.txt"
       |Content-Type: text/plain
       |
       |
       |
       |--aabbccddee
       |Content-Disposition: form-data; name="empty_file_middle"; filename="empty_file_followed_by_other_part.txt"
       |Content-Type: text/plain
       |
       |
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
       |--aabbccddee
       |Content-Disposition: file; name="file3"; filename="file3.txt"
       |Content-Type: text/plain
       |
       |the third file (with 'Content-Disposition: file' instead of 'form-data' as used in webhook callbacks of some scanners, see issue #8527)
       |
       |--aabbccddee
       |Content-Disposition: form-data; name="file4"; filename=""
       |Content-Type: application/octet-stream
       |
       |the fourth file (with empty filename)
       |
       |--aabbccddee
       |Content-Disposition: form-data; name="file5"; filename=
       |Content-Type: application/octet-stream
       |
       |the fifth file (with empty filename)
       |
       |--aabbccddee
       |Content-Disposition: form-data; name="empty_file_empty_filename"; filename=""
       |Content-Type: application/octet-stream
       |
       |
       |--aabbccddee
       |Content-Disposition: form-data; name="empty_file_bottom"; filename="empty_file_not_followed_by_any_other_part.txt"
       |Content-Type: text/plain
       |
       |
       |--aabbccddee--
       |""".stripMargin.linesIterator.mkString("\r\n")

  def parse(implicit app: Application) = app.injector.instanceOf[PlayBodyParsers]

  def checkResult(result: Either[Result, MultipartFormData[TemporaryFile]]) = {
    result must beRight.like {
      case parts =>
        parts.dataParts must haveLength(9)
        parts.dataParts.get("text1") must beSome(Seq("the first text field"))
        parts.dataParts.get("text2:colon") must beSome(Seq("the second text field"))
        parts.dataParts.get("noQuotesText1") must beSome(Seq("text field with unquoted name"))
        parts.dataParts.get("noQuotesText1:colon") must beSome(Seq("text field with unquoted name and colon"))
        parts.dataParts.get("empty_text") must beSome(Seq(""))
        parts.dataParts.get("") must beSome(Seq("empty name should work"))
        parts.dataParts.get("arr[]").get must contain(("array value 0"))
        parts.dataParts.get("arr[]").get must contain(("array value 1"))
        parts.dataParts.get("orderedarr[0]") must beSome(Seq("ordered array value 0"))
        parts.dataParts.get("orderedarr[1]") must beSome(Seq("ordered array value 1"))
        parts.files must haveLength(5)
        parts.file("file1") must beSome.like {
          case filePart => {
            PlayIO.readFileAsString(filePart.ref) must_== "the first file\r\n"
            filePart.fileSize must_== 16
          }
        }
        parts.file("file2") must beSome.like {
          case filePart => {
            PlayIO.readFileAsString(filePart.ref) must_== "the second file\r\n"
            filePart.fileSize must_== 17
          }
        }
        parts.file("file3") must beSome.like {
          case filePart => {
            PlayIO.readFileAsString(filePart.ref) must_== "the third file (with 'Content-Disposition: file' instead of 'form-data' as used in webhook callbacks of some scanners, see issue #8527)\r\n"
            filePart.fileSize must_== 137
          }
        }
        parts.file("file_with_space_only") must beSome.like {
          case filePart => PlayIO.readFileAsString(filePart.ref) must_== " "
        }
        parts.file("file_with_newline_only") must beSome.like {
          case filePart => PlayIO.readFileAsString(filePart.ref) must_== "\r\n"
        }
        parts.badParts must haveLength(5)
        parts.badParts must contain(
          (BadPart(
            Map(
              "content-disposition" -> """form-data; name="file4"; filename=""""",
              "content-type"        -> "application/octet-stream"
            )
          ))
        )
        parts.badParts must contain(
          (BadPart(
            Map(
              "content-disposition" -> """form-data; name="file5"; filename=""",
              "content-type"        -> "application/octet-stream"
            )
          ))
        )
        parts.badParts must contain(
          (BadPart(
            Map(
              "content-disposition" -> """form-data; name="empty_file_middle"; filename="empty_file_followed_by_other_part.txt"""",
              "content-type"        -> "text/plain"
            )
          ))
        )
        parts.badParts must contain(
          (BadPart(
            Map(
              "content-disposition" -> """form-data; name="empty_file_empty_filename"; filename=""""",
              "content-type"        -> "application/octet-stream"
            )
          ))
        )
        parts.badParts must contain(
          (BadPart(
            Map(
              "content-disposition" -> """form-data; name="empty_file_bottom"; filename="empty_file_not_followed_by_any_other_part.txt"""",
              "content-type"        -> "text/plain"
            )
          ))
        )
    }
  }

  def checkResultEmptyFileAllowed(result: Either[Result, MultipartFormData[TemporaryFile]]) = {
    result must beRight.like {
      case parts =>
        parts.dataParts must haveLength(9)
        parts.dataParts.get("text1") must beSome(Seq("the first text field"))
        parts.dataParts.get("text2:colon") must beSome(Seq("the second text field"))
        parts.dataParts.get("noQuotesText1") must beSome(Seq("text field with unquoted name"))
        parts.dataParts.get("noQuotesText1:colon") must beSome(Seq("text field with unquoted name and colon"))
        parts.dataParts.get("empty_text") must beSome(Seq(""))
        parts.dataParts.get("") must beSome(Seq("empty name should work"))
        parts.dataParts.get("arr[]").get must contain(("array value 0"))
        parts.dataParts.get("arr[]").get must contain(("array value 1"))
        parts.dataParts.get("orderedarr[0]") must beSome(Seq("ordered array value 0"))
        parts.dataParts.get("orderedarr[1]") must beSome(Seq("ordered array value 1"))
        parts.files must haveLength(10)
        parts.file("file1") must beSome.like {
          case filePart => {
            PlayIO.readFileAsString(filePart.ref) must_== "the first file\r\n"
            filePart.fileSize must_== 16
          }
        }
        parts.file("file2") must beSome.like {
          case filePart => {
            PlayIO.readFileAsString(filePart.ref) must_== "the second file\r\n"
            filePart.fileSize must_== 17
          }
        }
        parts.file("file3") must beSome.like {
          case filePart => {
            PlayIO.readFileAsString(filePart.ref) must_== "the third file (with 'Content-Disposition: file' instead of 'form-data' as used in webhook callbacks of some scanners, see issue #8527)\r\n"
            filePart.fileSize must_== 137
          }
        }
        parts.file("file_with_space_only") must beSome.like {
          case filePart => PlayIO.readFileAsString(filePart.ref) must_== " "
        }
        parts.file("file_with_newline_only") must beSome.like {
          case filePart => PlayIO.readFileAsString(filePart.ref) must_== "\r\n"
        }
        parts.file("empty_file_middle") must beSome.like {
          case filePart => {
            PlayIO.readFileAsString(filePart.ref) must_== ""
            filePart.fileSize must_== 0
            filePart.filename must_== "empty_file_followed_by_other_part.txt"
          }
        }
        parts.file("file4") must beSome.like {
          case filePart => {
            PlayIO.readFileAsString(filePart.ref) must_== "the fourth file (with empty filename)\r\n"
            filePart.fileSize must_== 39
            filePart.filename must_== ""
          }
        }
        parts.file("file5") must beSome.like {
          case filePart => {
            PlayIO.readFileAsString(filePart.ref) must_== "the fifth file (with empty filename)\r\n"
            filePart.fileSize must_== 38
            filePart.filename must_== ""
          }
        }
        parts.file("empty_file_empty_filename") must beSome.like {
          case filePart => {
            PlayIO.readFileAsString(filePart.ref) must_== ""
            filePart.fileSize must_== 0
            filePart.filename must_== ""
          }
        }
        parts.file("empty_file_bottom") must beSome.like {
          case filePart => {
            PlayIO.readFileAsString(filePart.ref) must_== ""
            filePart.fileSize must_== 0
            filePart.filename must_== "empty_file_not_followed_by_any_other_part.txt"
          }
        }
        parts.badParts must haveLength(0)
    }
  }

  def withClientAndServer[T](totalSpace: Long)(block: WSClient => T) = {
    Server.withApplicationFromContext() { context =>
      new BuiltInComponentsFromContext(context) with NoHttpFiltersComponents {
        override lazy val tempFileCreator: TemporaryFileCreator = new InMemoryTemporaryFileCreator(totalSpace)

        import play.api.routing.sird.{ POST => SirdPost, _ }
        override def router: Router = Router.from {
          case SirdPost(p"/") =>
            defaultActionBuilder(parse.multipartFormData) { request =>
              Results.Ok(request.body.files.map(_.filename).mkString(", "))
            }
        }
      }.application
    } { implicit port =>
      withClient(block)
    }
  }

  "The multipart/form-data parser" should {
    "parse some content" in new WithApplication() {
      val parser = parse.multipartFormData.apply(
        FakeRequest().withHeaders(
          CONTENT_TYPE -> "multipart/form-data; boundary=aabbccddee"
        )
      )

      val result = await(parser.run(Source.single(ByteString(body))))

      checkResult(result)
    }

    "parse some content with empty file allowed" in new WithApplication() {
      val parser = parse
        .multipartFormData(allowEmptyFiles = true)
        .apply(
          FakeRequest().withHeaders(
            CONTENT_TYPE -> "multipart/form-data; boundary=aabbccddee"
          )
        )

      val result = await(parser.run(Source.single(ByteString(body))))

      checkResultEmptyFileAllowed(result)
    }

    "parse some content that arrives one byte at a time" in new WithApplication() {
      val parser = parse.multipartFormData.apply(
        FakeRequest().withHeaders(
          CONTENT_TYPE -> "multipart/form-data; boundary=aabbccddee"
        )
      )

      val bytes  = body.getBytes.map(byte => ByteString(byte)).toVector
      val result = await(parser.run(Source(bytes)))

      checkResult(result)
    }

    "parse some content that arrives one byte at a time with empty file allowed" in new WithApplication() {
      val parser = parse
        .multipartFormData(allowEmptyFiles = true)
        .apply(
          FakeRequest().withHeaders(
            CONTENT_TYPE -> "multipart/form-data; boundary=aabbccddee"
          )
        )

      val bytes  = body.getBytes.map(byte => ByteString(byte)).toVector
      val result = await(parser.run(Source(bytes)))

      checkResultEmptyFileAllowed(result)
    }

    "return bad request for invalid body" in new WithApplication() {
      val parser = parse.multipartFormData.apply(
        FakeRequest().withHeaders(
          CONTENT_TYPE -> "multipart/form-data" // no boundary
        )
      )

      val result = await(parser.run(Source.single(ByteString(body))))

      result must beLeft.like {
        case error => error.header.status must_== BAD_REQUEST
      }
    }

    "validate the full length of the body" in new WithApplication(
      _.configure("play.http.parser.maxDiskBuffer" -> "100")
    ) {
      val parser = parse.multipartFormData.apply(
        FakeRequest().withHeaders(
          CONTENT_TYPE -> "multipart/form-data; boundary=aabbccddee"
        )
      )

      val result = await(parser.run(Source.single(ByteString(body))))

      result must beLeft.like {
        case error => error.header.status must_== REQUEST_ENTITY_TOO_LARGE
      }
    }

    "not parse more than the max data length" in new WithApplication(
      _.configure("play.http.parser.maxMemoryBuffer" -> "30")
    ) {
      val parser = parse.multipartFormData.apply(
        FakeRequest().withHeaders(
          CONTENT_TYPE -> "multipart/form-data; boundary=aabbccddee"
        )
      )

      val result = await(parser.run(Source.single(ByteString(body))))

      result must beLeft.like {
        case error => error.header.status must_== REQUEST_ENTITY_TOO_LARGE
      }
    }

    "return server internal error when file upload fails because temporary file creator fails" in withClientAndServer(
      1 /* super small total space */
    ) { ws =>
      val fileBody: ByteString                        = ByteString.fromString("the file body")
      val sourceFileBody: Source[ByteString, NotUsed] = Source.single(fileBody)
      val filePart: FilePart[Source[ByteString, NotUsed]] = FilePart(
        key = "file",
        filename = "file.txt",
        contentType = Option("text/plain"),
        ref = sourceFileBody,
        fileSize = fileBody.size
      )

      val response = ws
        .url("/")
        .post(Source.single(filePart))

      val res = await(response)
      res.status must_== INTERNAL_SERVER_ERROR
    }

    "work if there's no crlf at the start" in new WithApplication() {
      val parser = parse.multipartFormData.apply(
        FakeRequest().withHeaders(
          CONTENT_TYPE -> "multipart/form-data; boundary=aabbccddee"
        )
      )

      val result = await(parser.run(Source.single(ByteString(body))))

      checkResult(result)
    }

    "parse headers with semicolon inside quotes" in {
      val result = FileInfoMatcher.unapply(
        Map(
          "content-disposition" -> """form-data; name="document"; filename="semicolon;inside.jpg"""",
          "content-type"        -> "image/jpeg"
        )
      )
      result must not(beEmpty)
      result.get must equalTo(("document", "semicolon;inside.jpg", Option("image/jpeg"), "form-data"))
    }

    "parse headers with escaped quote inside quotes" in {
      val result = FileInfoMatcher.unapply(
        Map(
          "content-disposition" -> """form-data; name="document"; filename="quotes\"\".jpg"""",
          "content-type"        -> "image/jpeg"
        )
      )
      result must not(beEmpty)
      result.get must equalTo(("document", """quotes"".jpg""", Option("image/jpeg"), "form-data"))
    }

    "parse unquoted content disposition with file matcher" in {
      val result =
        FileInfoMatcher.unapply(Map("content-disposition" -> """form-data; name=document; filename=hello.txt"""))
      result must not(beEmpty)
      result.get must equalTo(("document", "hello.txt", None, "form-data"))
    }

    "parse unquoted content disposition with part matcher" in {
      val result = PartInfoMatcher.unapply(Map("content-disposition" -> """form-data; name=partName"""))
      result must not(beEmpty)
      result.get must equalTo("partName")
    }

    "parse extended name in content disposition" in {
      val result = PartInfoMatcher.unapply(
        Map("content-disposition" -> """form-data; name=partName; name*=utf8'en'extendedName""")
      )
      result must not(beEmpty)
      result.get must equalTo("extendedName")
    }

    "parse extended filename in content disposition" in {
      val result = FileInfoMatcher.unapply(
        Map(
          "content-disposition" -> """form-data; name=document; filename=hello.txt; filename*=utf-8''%E4%BD%A0%E5%A5%BD.txt"""
        )
      )
      result must not(beEmpty)
      result.get must equalTo(("document", "你好.txt", None, "form-data"))
    }

    "accept also 'Content-Disposition: file' for file as used in webhook callbacks of some scanners (see issue #8527)" in {
      val result = FileInfoMatcher.unapply(Map("content-disposition" -> """file; name=document; filename=hello.txt"""))
      result must not(beEmpty)
      result.get must equalTo(("document", "hello.txt", None, "file"))
    }
  }
}
