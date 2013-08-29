package play.filters.gzip

import concurrent.Await
import play.api.libs.iteratee.{Iteratee, Enumeratee, Enumerator}
import concurrent.duration.Duration
import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext.Implicits._

object GzipSpec extends Specification {

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
      val check: Array[Byte] = Await.result(Enumerator.fromStream(is) |>>> Iteratee.consume[Array[Byte]](), Duration.Inf)
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

    /**
     * Uses the gzip enumeratee to verify correctness.
     */
    def test(value: String, gunzip: Enumeratee[Array[Byte], Array[Byte]] = Gzip.gunzip(), gzip: Enumeratee[Array[Byte], Array[Byte]] = Gzip.gzip()) = {
      val future = Enumerator(value.getBytes("utf-8")) &> gzip &> gunzip |>>> Iteratee.consume[Array[Byte]]()
      val result = new String(Await.result(future, Duration.Inf), "utf-8")
      result must_== value
    }

    "gunzip simple input" in {
      test("Hello world")
    }

    "gunzip simple input in small chunks" in {
      test("Hello world", gzip = Gzip.gzip(5))
    }

    "gunzip large repeating input" in {
      test(Seq.fill(1000)("Hello world").mkString(""))
    }

    "gunzip large repeating input in small chunks" in {
      test(Seq.fill(1000)("Hello world").mkString(""), gzip = Gzip.gzip(100))
    }

    "gunzip large random input" in {
      test(scala.util.Random.nextString(10000))
    }

    "gunzip large random input in small chunks" in {
      test(scala.util.Random.nextString(10000), gzip = Gzip.gzip(100))
    }
  }
}
