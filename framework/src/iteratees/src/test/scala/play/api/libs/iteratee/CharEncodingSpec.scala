package play.api.libs.iteratee

import org.specs2.mutable._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import scala.concurrent.ExecutionContext.Implicits.global

object CharEncodingSpec extends Specification {

  "CharEncodingSpec.decode()" should {

    "decode US-ASCII" in {
      val input = Seq(
        Array[Byte](0x48, 0x65, 0x6c),
        Array[Byte](0x6c, 0x6f, 0x20, 0x57),
        Array[Byte](0x6f, 0x72, 0x6c, 0x64)
      )
      val result = Enumerator(input: _*) &> CharEncoding.decode("US-ASCII") |>>> Iteratee.consume[String]()
      Await.result(result, Duration.Inf) must be equalTo new String(input.flatten.toArray, "US-ASCII")
    }

    "decode UTF-8" in {
      val input = Seq(
        Array[Byte](0x48, 0x65, 0x6c),
        Array[Byte](0x6c, 0x6f, 0x20, 0x57),
        Array[Byte](0x6f, 0x72, 0x6c, 0x64)
      )
      val result = Enumerator(input: _*) &> CharEncoding.decode("UTF-8") |>>> Iteratee.consume[String]()
      Await.result(result, Duration.Inf) must be equalTo new String(input.flatten.toArray, "UTF-8")
    }

    "decode UTF-8 with split characters" in {
      val input = Seq(
        Array[Byte](0xe2.toByte),
        Array[Byte](0x82.toByte),
        Array[Byte](0xac.toByte),
        Array[Byte](0xf0.toByte),
        Array[Byte](0x9f.toByte),
        Array[Byte](0x82.toByte),
        Array[Byte](0xA5.toByte)
      )
      val result = Enumerator(input: _*) &> CharEncoding.decode("UTF-8") |>>> Iteratee.consume[String]()
      Await.result(result, Duration.Inf) must be equalTo "\u20ac\ud83c\udca5"
    }

    "decode UTF-16" in {
      val input = Seq(
        Array[Byte](0x00, 0x48, 0x00, 0x65, 0x00, 0x6c),
        Array[Byte](0x00, 0x6c, 0x00, 0x6f, 0x00, 0x20, 0x00, 0x57),
        Array[Byte](0x00, 0x6f, 0x00, 0x72, 0x00, 0x6c, 0x00, 0x64)
      )
      val result = Enumerator(input: _*) &> CharEncoding.decode("UTF-16") |>>> Iteratee.consume[String]()
      Await.result(result, Duration.Inf) must be equalTo "Hello World"
    }

    "decode UTF-16 with split characters" in {
      val input = Seq(
        Array[Byte](0x20),
        Array[Byte](0xac.toByte),
        Array[Byte](0xd8.toByte),
        Array[Byte](0x3c),
        Array[Byte](0xdc.toByte),
        Array[Byte](0xa5.toByte)
      )
      val result = Enumerator(input: _*) &> CharEncoding.decode("UTF-16") |>>> Iteratee.consume[String]()
      Await.result(result, Duration.Inf) must be equalTo "\u20ac\ud83c\udca5"
    }

    "decode UTF-32" in {
      val input = Seq(
        Array[Byte](0x00, 0x00, 0x00, 0x48, 0x00, 0x00, 0x00, 0x65, 0x00, 0x00, 0x00, 0x6c),
        Array[Byte](0x00, 0x00, 0x00, 0x6c, 0x00, 0x00, 0x00, 0x6f, 0x00, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x57),
        Array[Byte](0x00, 0x00, 0x00, 0x6f, 0x00, 0x00, 0x00, 0x72, 0x00, 0x00, 0x00, 0x6c, 0x00, 0x00, 0x00, 0x64)
      )
      val result = Enumerator(input: _*) &> CharEncoding.decode("UTF-32") |>>> Iteratee.consume[String]()
      Await.result(result, Duration.Inf) must be equalTo "Hello World"
    }

    "decode UTF-32 with split characters" in {
      val input = Seq(
        Array[Byte](0x00),
        Array[Byte](0x00),
        Array[Byte](0x20),
        Array[Byte](0xac.toByte),
        Array[Byte](0x00),
        Array[Byte](0x01),
        Array[Byte](0xf0.toByte),
        Array[Byte](0xa5.toByte)
      )
      val result = Enumerator(input: _*) &> CharEncoding.decode("UTF-32") |>>> Iteratee.consume[String]()
      Await.result(result, Duration.Inf) must be equalTo "\u20ac\ud83c\udca5"
    }

    "fail on invalid ASCII" in {
      val input = Seq(
        Array[Byte](0x80.toByte)
      )
      val result = Enumerator(input: _*) &> CharEncoding.decode("US-ASCII") |>>> Iteratee.skipToEof
      val status = result.map { _ => "success" }.recover { case e => "failure" }
      Await.result(status, Duration.Inf) must be equalTo "failure"
    }

    "fail on invalid UTF-8" in {
      val input = Seq(
        Array[Byte](0xe2.toByte, 0xe2.toByte, 0xe2.toByte)
      )
      val result = Enumerator(input: _*) &> CharEncoding.decode("UTF-8") |>>> Iteratee.skipToEof
      val status = result.map { _ => "success" }.recover { case e => "failure" }
      Await.result(status, Duration.Inf) must be equalTo "failure"
    }

    "fail on invalid UTF-16" in {
      val input = Seq(
        Array[Byte](0xd8.toByte, 0x00),
        Array[Byte](0xd8.toByte, 0x00)
      )
      val result = Enumerator(input: _*) &> CharEncoding.decode("UTF-16") |>>> Iteratee.skipToEof
      val status = result.map { _ => "success" }.recover { case e => "failure" }
      Await.result(status, Duration.Inf) must be equalTo "failure"
    }

    "fail on invalid UTF-32" in {
      val input = Seq(
        Array[Byte](0x00)
      )
      val result = Enumerator(input: _*) &> CharEncoding.decode("UTF-32") |>>> Iteratee.skipToEof
      val status = result.map { _ => "success" }.recover { case e => "failure" }
      Await.result(status, Duration.Inf) must be equalTo "failure"
    }

  }

  "CharEncodingSpec.encode()" should {

    "encode US-ASCII" in {
      val input = Seq(
        "Hel",
        "lo W",
        "orld"
      )
      val result = Enumerator(input: _*) &> CharEncoding.encode("US-ASCII") |>>> Iteratee.consume[Array[Byte]]()
      Await.result(result, Duration.Inf) must be equalTo input.mkString.getBytes("US-ASCII")
    }

    "encode UTF-8" in {
      val input = Seq(
        "Hel",
        "lo W",
        "orld"
      )
      val result = Enumerator(input: _*) &> CharEncoding.encode("UTF-8") |>>> Iteratee.consume[Array[Byte]]()
      Await.result(result, Duration.Inf) must be equalTo input.mkString.getBytes("UTF-8")
    }

    "encode UTF-8 with split characters" in {
      val input = Seq(
        "\u20ac",
        "\ud83c",
        "\udca5"
      )
      val result = Enumerator(input: _*) &> CharEncoding.encode("UTF-8") |>>> Iteratee.consume[Array[Byte]]()
      Await.result(result, Duration.Inf) must be equalTo input.mkString.getBytes("UTF-8")
    }

    "encode UTF-16" in {
      val input = Seq(
        "Hel",
        "lo W",
        "orld"
      )
      val result = Enumerator(input: _*) &> CharEncoding.encode("UTF-16") |>>> Iteratee.consume[Array[Byte]]()
      Await.result(result, Duration.Inf) must be equalTo input.mkString.getBytes("UTF-16BE")
    }

    "encode UTF-16 with split characters" in {
      val input = Seq(
        "\u20ac",
        "\ud83c",
        "\udca5"
      )
      val result = Enumerator(input: _*) &> CharEncoding.encode("UTF-16") |>>> Iteratee.consume[Array[Byte]]()
      Await.result(result, Duration.Inf) must be equalTo input.mkString.getBytes("UTF-16BE")
    }

    "encode UTF-32" in {
      val input = Seq(
        "Hel",
        "lo W",
        "orld"
      )
      val result = Enumerator(input: _*) &> CharEncoding.encode("UTF-32") |>>> Iteratee.consume[Array[Byte]]()
      Await.result(result, Duration.Inf) must be equalTo input.mkString.getBytes("UTF-32")
    }

    "encode UTF-32 with split characters" in {
      val input = Seq(
        "\u20ac",
        "\ud83c",
        "\udca5"
      )
      val result = Enumerator(input: _*) &> CharEncoding.encode("UTF-32") |>>> Iteratee.consume[Array[Byte]]()
      Await.result(result, Duration.Inf) must be equalTo input.mkString.getBytes("UTF-32")
    }

    "fail on unmappable ASCII" in {
      val input = Seq("\u20ac")
      val result = Enumerator(input: _*) &> CharEncoding.encode("US-ASCII") |>>> Iteratee.skipToEof
      val status = result.map { _ => "success" }.recover { case e => "failure" }
      Await.result(status, Duration.Inf) must be equalTo "failure"
    }

    "fail on invalid Unicode with UTF-8" in {
      val input = Seq(
        "\ud83c"
      )
      val result = Enumerator(input: _*) &> CharEncoding.encode("UTF-8") |>>> Iteratee.skipToEof
      val status = result.map { _ => "success" }.recover { case e => "failure" }
      Await.result(status, Duration.Inf) must be equalTo "failure"
    }

    "fail on invalid Unicode with UTF-8" in {
      val input = Seq(
        "\ud83c"
      )
      val result = Enumerator(input: _*) &> CharEncoding.encode("UTF-16") |>>> Iteratee.skipToEof
      val status = result.map { _ => "success" }.recover { case e => "failure" }
      Await.result(status, Duration.Inf) must be equalTo "failure"
    }

    "fail on invalid Unicode with UTF-32" in {
      val input = Seq(
        "\ud83c"
      )
      val result = Enumerator(input: _*) &> CharEncoding.encode("UTF-32") |>>> Iteratee.skipToEof
      val status = result.map { _ => "success" }.recover { case e => "failure" }
      Await.result(status, Duration.Inf) must be equalTo "failure"
    }

  }

}
