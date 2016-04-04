/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.ByteString
import org.specs2.mutable._
import play.api.http.HttpEntity
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object RangeResultsSpec extends Specification {

  "Result" should {

    "have status" in {
      val stream = new java.io.ByteArrayInputStream(Array[Byte](1, 2, 3))
      val Result(ResponseHeader(status, _, _), _) = RangeResult.ofStream(stream, None, None)
      status must_== 200
    }

    "have headers" in {
      val stream = new java.io.ByteArrayInputStream(Array[Byte](1, 2, 3))
      val Result(ResponseHeader(_, headers, _), _) = RangeResult.ofStream(stream, None, None, None)
      headers must havePair("Accept-Ranges" -> "bytes")
      headers must havePair("Content-Length" -> "3")
      headers must havePair("Content-Type" -> "application/octet-stream")
    }

    "support Content-Disposition header" in {
      val stream = new java.io.ByteArrayInputStream(Array[Byte](1, 2, 3))
      val Result(ResponseHeader(_, headers, _), _) = RangeResult.ofStream(stream, None, Some("video.mp4"), None)
      headers must havePair("Content-Disposition" -> "attachment; filename=\"video.mp4\"")
    }

    "support first byte position" in {
      val stream = new java.io.ByteArrayInputStream(Array[Byte](1, 2, 3))
      val Result(ResponseHeader(_, headers, _), HttpEntity.Streamed(data, _, _)) = RangeResult.ofStream(stream, Some("bytes=1-"), None)
      headers must havePair("Content-Range" -> "bytes 1-2/3")
      headers must havePair("Content-Length" -> "2")
      implicit val system = ActorSystem()
      implicit val materializer = ActorMaterializer()
      val result = Await.result(data.runFold(ByteString.empty)(_ ++ _).map(_.toArray), Duration.Inf)
      mutable.WrappedArray.make(result) must be_==(mutable.WrappedArray.make(Array[Byte](2, 3)))
    }

    "support last byte position" in {
      val stream = new java.io.ByteArrayInputStream(Array[Byte](1, 2, 3, 4, 5, 6))
      val Result(ResponseHeader(_, headers, _), HttpEntity.Streamed(data, _, _)) = RangeResult.ofStream(stream, Some("bytes=2-4"), None)
      headers must havePair("Content-Range" -> "bytes 2-4/6")
      headers must havePair("Content-Length" -> "3")
      implicit val system = ActorSystem()
      implicit val materializer = ActorMaterializer()
      val result = Await.result(data.runFold(ByteString.empty)(_ ++ _).map(_.toArray), Duration.Inf)
      mutable.WrappedArray.make(result) must be_==(mutable.WrappedArray.make(Array[Byte](3, 4, 5)))
    }

    "support sending path" in {
      val path = java.nio.file.Paths.get("path.mp4")
      java.nio.file.Files.createFile(path)
      try {
        val Result(ResponseHeader(_, headers, _), _) = RangeResult.ofPath(path, None, Some("video/mp4"))
        headers must havePair("Content-Disposition" -> "attachment; filename=\"path.mp4\"")
        headers must havePair("Content-Type" -> "video/mp4")
      } finally {
        java.nio.file.Files.delete(path)
      }
    }

    "support sending file" in {
      val file = new java.io.File("file.mp4")
      file.createNewFile()
      try {
        val Result(ResponseHeader(_, headers, _), _) = RangeResult.ofFile(file, None, Some("video/mp4"))
        headers must havePair("Content-Disposition" -> "attachment; filename=\"file.mp4\"")
        headers must havePair("Content-Type" -> "video/mp4")
      } finally {
        file.delete()
      }
    }

  }

}
