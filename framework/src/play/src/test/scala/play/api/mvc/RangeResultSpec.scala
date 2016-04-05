/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import java.io.{ File, FileOutputStream }
import java.nio.file.Path

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.StreamConverters
import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import org.specs2.mutable.Specification

object ByteRangeSpec extends Specification {

  "Distance" in {
    "Between 0-10 and 20-30 is 10" in {
      val byteRange1 = ByteRange(0, 10)
      val byteRange2 = ByteRange(20, 30)
      byteRange1.distance(byteRange2) must beEqualTo(10)
    }
    "Between 0-10 and 10-20 is zero" in {
      val byteRange1 = ByteRange(0, 10)
      val byteRange2 = ByteRange(10, 20)
      byteRange1.distance(byteRange2) must beEqualTo(0)
    }
    "Between 0-10 and 5-15 is zero" in {
      val byteRange1 = ByteRange(0, 10)
      val byteRange2 = ByteRange(10, 20)
      byteRange1.distance(byteRange2) must beEqualTo(0)
    }
    "Between 0-100 and 20-80 is zero" in {
      val byteRange1 = ByteRange(0, 10)
      val byteRange2 = ByteRange(10, 20)
      byteRange1.distance(byteRange2) must beEqualTo(0)
    }
  }
}

object RangeSpec extends Specification {

  def checkRange(entityLength: Long, header: String, expected: Range) = {
    val range = Range(entityLength, header)
    range must beSome[Range]
    range must beSome.which(_.entityLength == expected.entityLength)
    range must beSome.which(_.byteRange == expected.byteRange)
  }

  "Satisfiable ranges" in {

    "0-10" in {
      checkRange(
        entityLength = 120,
        header = "0-10",
        expected = Range(entityLength = 120, Some(0), Some(10))
      )
    }
    "80-100" in {
      checkRange(
        entityLength = 120,
        header = "80-100",
        expected = Range(entityLength = 120, Some(80), Some(100))
      )
    }
    "80-" in {
      checkRange(
        entityLength = 120,
        header = "80-",
        expected = Range(entityLength = 120, Some(80), Some(119))
      )
    }

    "\\-100" in {
      checkRange(
        entityLength = 120,
        header = "-100",
        expected = Range(entityLength = 120, Some(20), Some(119))
      )
    }

    "Ending ranges" in {
      "The last -20 bytes" in {
        checkRange(
          entityLength = 120,
          header = "-20",
          expected = Range(entityLength = 120, Some(100), Some(119))
        )
      }
    }

    "Replaces the value of last-byte-pos with one less than the current length" in {
      "When last-byte-pos value is absent" in {
        checkRange(
          entityLength = 120,
          header = "100-",
          expected = Range(entityLength = 120, Some(100), Some(119))
        )
      }
      "When last-byte-pos value is greater than entity length" in {
        checkRange(
          entityLength = 120,
          header = "100-140",
          expected = Range(entityLength = 120, Some(100), Some(119))
        )
      }
      "When last-byte-pos value is equal to entity length" in {
        checkRange(
          entityLength = 120,
          header = "100-120",
          expected = Range(entityLength = 120, Some(100), Some(119))
        )
      }
    }
  }

  "Unsatisfiable ranges" in {
    "When both first and last bytes are not specified" in {
      Range(entityLength = 100, range = "-") must beNone
    }

    "When range header is empty" in {
      Range(entityLength = 100, range = "") must beNone
    }
  }

  "Ordering ranges" in {
    "by first byte" in {
      val range1 = Range(120, "0-10")
      val range2 = Range(120, "10-20")
      range1 must beLessThan(range2)
    }

    "by last byte when first bytes are equals" in {
      val range1 = Range(120, "0-20")
      val range2 = Range(120, "0-21")
      range1 must beLessThan(range2)
    }

    "when first byte is not specified" in {
      "first the range with a first byte specified" in {
        val range1 = Range(120, "10-20")
        val range2 = Range(120, "-21")
        range1 must beLessThan(range2)
      }

      "first the range that selects more bytes starting from the end" in {
        val range1 = Range(120, "-30")
        val range2 = Range(120, "-20")
        range1 must beLessThan(range2)
      }
    }
  }

  "Content length" in {
    "500-999 has content-length = 500" in {
      val range = Range(entityLength = 1000, range = "500-999")
      range must beSome.which(_.length == 500)
    }
    "0-499 has content-length = 500" in {
      val range = Range(entityLength = 1000, range = "0-499")
      range must beSome.which(_.length == 500)
    }
    "0-10 has content-length = 11" in {
      val range = Range(entityLength = 1000, range = "0-10")
      range must beSome.which(_.length == 11)
    }
    "with range 9500- and 10000 bytes available has content-length = 500" in {
      val range = Range(entityLength = 10000, range = "9500-")
      range must beSome.which(_.length == 500)
    }
  }

  "Merge ranges" in {
    val range1 = Range(entityLength = 10000, range = "0-10")
    val range2 = Range(entityLength = 10000, range = "5-15")
    val merged = (range1, range2) match {
      case (Some(r1), Some(r2)) => r1.merge(r2)
    }
    merged.start must beSome(0)
    merged.end must beSome(15)
  }

  "Validate ranges" in {
    "Invalid when" in {
      "last-byte-pos value less than its first-byte-pos" in {
        val range = Range(entityLength = 10000, range = "200-100")
        range must beSome.which(_.isValid == false)
      }
      "first-byte-pos greater than entity length" in {
        val range = Range(entityLength = 1000, range = "2000-3000")
        range must beSome.which(_.isValid == false)
      }
    }
    "Valid" in {
      "last-byte-pos is equal to first-byte-pos" in {
        val range = Range(entityLength = 10000, range = "100-100")
        range must beSome.which(_.isValid == true)
      }
      "first-byte-pos is less than entity length" in {
        val range = Range(entityLength = 10000, range = "200-300")
        range must beSome.which(_.isValid == true)
      }
    }
  }
}

object RangeSetSpec extends Specification {

  "Satisfiable range sets" in {

    "bytes=0-5,100-110" in {
      val rangeSet = RangeSet(entityLength = 120, rangeHeader = Some("bytes=0-5,100-110"))
      rangeSet must beAnInstanceOf[SatisfiableRangeSet]
      rangeSet.entityLength must beEqualTo(120)
      rangeSet.toString must beEqualTo("bytes 0-5,100-110/120")
    }

    "bytes=0-0,-1" in {
      val rangeSet = RangeSet(entityLength = 120, rangeHeader = Some("bytes=0-0,-1"))
      rangeSet must beAnInstanceOf[SatisfiableRangeSet]
      rangeSet.entityLength must beEqualTo(120)
      rangeSet.toString must beEqualTo("bytes 0-0,119-119/120")
    }

    "bytes=500-600,801-999" in {
      val rangeSet = RangeSet(entityLength = 1200, rangeHeader = Some("bytes=500-600,801-999"))
      rangeSet must beAnInstanceOf[SatisfiableRangeSet]
      rangeSet.entityLength must beEqualTo(1200)
      rangeSet.toString must beEqualTo("bytes 500-600,801-999/1200")
    }

    "Normalize" in {
      "bytes=500-600,601-650,1000-1100 to bytes=500-650,1000-1100" in {
        val rangeSet = RangeSet(entityLength = 1200, rangeHeader = Some("bytes=500-600,601-650,1000-1100"))
        rangeSet must beAnInstanceOf[SatisfiableRangeSet]
        rangeSet.toString must beEqualTo("bytes 500-650,1000-1100/1200")
      }
      "bytes=500-600,601-650,680-700,1000-1100 to bytes=500-700,1000-1100" in {
        val rangeSet = RangeSet(entityLength = 1200, rangeHeader = Some("bytes=500-600,601-650,680-700,1000-1100"))
        rangeSet must beAnInstanceOf[SatisfiableRangeSet]
        rangeSet.toString must beEqualTo("bytes 500-700,1000-1100/1200")
      }
      "bytes=500-600,400-650,680-700,1000- to bytes=400-700,1000-1199" in {
        val rangeSet = RangeSet(entityLength = 1200, rangeHeader = Some("bytes=500-600,400-650,680-700,1000-"))
        rangeSet must beAnInstanceOf[SatisfiableRangeSet]
        rangeSet.toString must beEqualTo("bytes 400-700,1000-1199/1200")
      }
      "bytes=500-600 to bytes=500-600" in {
        val rangeSet = RangeSet(entityLength = 1200, rangeHeader = Some("bytes=500-600"))
        rangeSet must beAnInstanceOf[SatisfiableRangeSet]
        rangeSet.toString must beEqualTo("bytes 500-600/1200")
      }
    }
  }

  "Unsatisfiable range sets" in {

    "When last-byte-pos less than first-byte-pos" in {
      val rangeSet = RangeSet(entityLength = 120, rangeHeader = Some("bytes=20-30,40-10"))
      rangeSet.entityLength must beEqualTo(120)
      rangeSet must beAnInstanceOf[UnsatisfiableRangeSet]
    }
    "When first-byte-pos more than entity length" in {
      val rangeSet = RangeSet(entityLength = 120, rangeHeader = Some("bytes=0-0,200-210"))
      rangeSet.entityLength must beEqualTo(120)
      rangeSet must beAnInstanceOf[UnsatisfiableRangeSet]
    }
  }

  "No header present" in {
    val rangeSet = RangeSet(entityLength = 120, rangeHeader = None)
    rangeSet.entityLength must beEqualTo(120)
    rangeSet must beAnInstanceOf[NoHeaderRangeSet]
  }
}

object RangeResultSpec extends Specification {

  "Result" should {

    "have status ok when there is no range" in {
      val stream = new java.io.ByteArrayInputStream(Array[Byte](1, 2, 3))
      val source = StreamConverters.fromInputStream(() => stream)
      val Result(ResponseHeader(status, _, _), _) = RangeResult.ofSource(stream.available(), source, None, None, None)
      status must_== 200
    }

    "have headers" in {
      val stream = new java.io.ByteArrayInputStream(Array[Byte](1, 2, 3))
      val source = StreamConverters.fromInputStream(() => stream)
      val Result(ResponseHeader(_, headers, _), _) = RangeResult.ofSource(stream.available(), source, None, None, None)
      headers must havePair("Accept-Ranges" -> "bytes")
      headers must havePair("Content-Type" -> "application/octet-stream")
    }

    "support Content-Disposition header" in {
      val stream = new java.io.ByteArrayInputStream(Array[Byte](1, 2, 3))
      val source = StreamConverters.fromInputStream(() => stream)
      val Result(ResponseHeader(_, headers, _), _) = RangeResult.ofSource(stream.available(), source, None, Some("video.mp4"), None)
      headers must havePair("Content-Disposition" -> "attachment; filename=\"video.mp4\"")
    }

    "support first byte position" in {
      val stream = new java.io.ByteArrayInputStream(Array[Byte](1, 2, 3))
      val source = StreamConverters.fromInputStream(() => stream)
      val Result(ResponseHeader(_, headers, _), HttpEntity.Streamed(data, _, _)) = RangeResult.ofSource(stream.available(), source, Some("bytes=1-"), None, None)
      headers must havePair("Content-Range" -> "bytes 1-2/3")

      implicit val system = ActorSystem()
      implicit val materializer = ActorMaterializer()
      val result = Await.result(data.runFold(ByteString.empty)(_ ++ _).map(_.toArray), Duration.Inf)
      mutable.WrappedArray.make(result) must be_==(mutable.WrappedArray.make(Array[Byte](2, 3)))
    }

    "support last byte position" in {
      val stream = new java.io.ByteArrayInputStream(Array[Byte](1, 2, 3, 4, 5, 6))
      val source = StreamConverters.fromInputStream(() => stream)
      val Result(ResponseHeader(_, headers, _), HttpEntity.Streamed(data, _, _)) = RangeResult.ofSource(stream.available(), source, Some("bytes=2-4"), None, None)
      headers must havePair("Content-Range" -> "bytes 2-4/6")
      implicit val system = ActorSystem()
      implicit val materializer = ActorMaterializer()
      val result = Await.result(data.runFold(ByteString.empty)(_ ++ _).map(_.toArray), Duration.Inf)
      mutable.WrappedArray.make(result) must be_==(mutable.WrappedArray.make(Array[Byte](3, 4, 5)))
    }

    "support sending path" in {
      val file = createFile(java.nio.file.Paths.get("path.mp4"))
      try {
        val Result(ResponseHeader(_, headers, _), _) = RangeResult.ofPath(file.toPath, None, Some("video/mp4"))
        headers must havePair("Content-Disposition" -> "attachment; filename=\"path.mp4\"")
        headers must havePair("Content-Type" -> "video/mp4")
      } finally {
        java.nio.file.Files.delete(file.toPath)
      }
    }

    "support sending file" in {
      val file = createFile(java.nio.file.Paths.get("file.mp4"))
      try {
        val Result(ResponseHeader(_, headers, _), _) = RangeResult.ofFile(file, None, Some("video/mp4"))
        headers must havePair("Content-Disposition" -> "attachment; filename=\"file.mp4\"")
        headers must havePair("Content-Type" -> "video/mp4")
      } finally {
        java.nio.file.Files.delete(file.toPath)
      }
    }
  }

  def createFile(path: Path): File = {
    if (!java.nio.file.Files.exists(path)) {
      java.nio.file.Files.createFile(path)
      val fos = new FileOutputStream(path.toFile)
      try {
        fos.write("The file content".getBytes)
      } finally {
        fos.close()
      }
    }
    path.toFile
  }
}