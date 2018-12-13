/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

import java.io.File

import akka.util.ByteString
import org.specs2.mutable.Specification
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{ Codec, MultipartFormData }
import play.api.mvc.MultipartFormData.FilePart

import play.api.libs.Files.SingletonTemporaryFileCreator._

class WriteableSpec extends Specification {

  "Writeable" in {

    "of multipart" should {

      "work for temporary files" in {
        val multipartFormData = createMultipartFormData[TemporaryFile](create(new File("src/test/resources/multipart-form-data-file.txt").toPath))
        val contentType = Some("text/plain")
        val codec = Codec.utf_8

        val writeable = Writeable.writeableOf_MultipartFormData(codec, contentType)
        val transformed: ByteString = writeable.transform(multipartFormData)

        transformed.utf8String must contain("Content-Disposition: form-data; name=name")
        transformed.utf8String must contain("""Content-Disposition: form-data; name="thefile"; filename="something.text"""")
        transformed.utf8String must contain("Content-Type: text/plain")
        transformed.utf8String must contain("multipart-form-data-file")
      }

      "work composing with another writeable" in {
        val multipartFormData = createMultipartFormData[String]("file part value")
        val contentType = Some("text/plain")
        val codec = Codec.utf_8

        val writeable = Writeable.writeableOf_MultipartFormData(
          codec,
          Writeable[FilePart[String]]((f: FilePart[String]) => codec.encode(f.ref), contentType)
        )
        val transformed: ByteString = writeable.transform(multipartFormData)

        transformed.utf8String must contain("Content-Disposition: form-data; name=name")
        transformed.utf8String must contain("""Content-Disposition: form-data; name="thefile"; filename="something.text"""")
        transformed.utf8String must contain("Content-Type: text/plain")
        transformed.utf8String must contain("file part value")
      }

      "use multipart/form-data content-type" in {
        val contentType = Some("text/plain")
        val codec = Codec.utf_8
        val writeable = Writeable.writeableOf_MultipartFormData(
          codec,
          Writeable[FilePart[String]]((f: FilePart[String]) => codec.encode(f.ref), contentType)
        )

        writeable.contentType must beSome(startWith("multipart/form-data; boundary="))
      }
    }
  }

  def createMultipartFormData[A](ref: A): MultipartFormData[A] = {
    MultipartFormData[A](
      dataParts = Map(
        "name" -> Seq("value")
      ),
      files = Seq(
        FilePart[A](
          key = "thefile",
          filename = "something.text",
          contentType = Some("text/plain"),
          ref = ref
        )
      ),
      badParts = Seq.empty
    )
  }
}
