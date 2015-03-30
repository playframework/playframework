/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.cache

import play.api.test._
import play.api.mvc.{ HttpConnection, Result, Results }
import play.api.libs.iteratee.{ Enumerator, Iteratee }

import scala.concurrent.ExecutionContext.Implicits.global

class SerializableResultSpec extends PlaySpecification {

  sequential

  "SerializableResult" should {

    def serializeAndDeserialize(result: Result): Result = {
      val inWrapper = new SerializableResult(result)
      import java.io._
      val baos = new ByteArrayOutputStream()
      val oos = new ObjectOutputStream(baos)
      oos.writeObject(inWrapper)
      oos.flush()
      oos.close()
      baos.close()
      val bytes = baos.toByteArray
      val bais = new ByteArrayInputStream(bytes)
      val ois = new ObjectInputStream(bais)
      val outWrapper = ois.readObject().asInstanceOf[SerializableResult]
      ois.close()
      bais.close()
      outWrapper.result
    }

    // To be fancy could use a Matcher
    def compareResults(r1: Result, r2: Result) = {
      def bodyChunks(r: Result): Vector[Vector[Byte]] = {
        await(r.body |>>> Iteratee.getChunks[Array[Byte]]).map(_.to[Vector]).to[Vector]
      }
      r1.header.status must_== r2.header.status
      r1.header.headers must_== r2.header.headers
      bodyChunks(r1) must_== bodyChunks(r2)
      r1.connection must_== r2.connection
    }

    def checkSerialization(r: Result) = {
      val r2 = serializeAndDeserialize(r)
      compareResults(r, r2)
    }

    "serialize and deserialize statūs" in {
      checkSerialization(Results.Ok("x").withHeaders(CONTENT_TYPE -> "text/banana"))
      checkSerialization(Results.NotFound)
    }
    "serialize and deserialize simple Results" in {
      checkSerialization(Results.Ok("hello!"))
      checkSerialization(Results.Ok.chunked(Enumerator("hel", "lo!")))
      checkSerialization(Results.Ok("hello!").withHeaders(CONTENT_TYPE -> "text/banana"))
      checkSerialization(Results.Ok("hello!").withHeaders(CONTENT_TYPE -> "text/banana", "X-Foo" -> "bar"))
    }
    "serialize and deserialize chunked Results" in {
      checkSerialization(Results.Ok.chunked(Enumerator("hel", "lo!")))
      checkSerialization(Results.Ok.chunked(Enumerator("hel", "lo", "!")))
    }
    "serialize and deserialize connection types" in {
      checkSerialization(Results.Ok("hello!").copy(connection = HttpConnection.KeepAlive))
      checkSerialization(Results.Ok("hello!").copy(connection = HttpConnection.Close))
    }
    "serialize and deserialize large results" in {
      val n = 200
      val chunks = Enumerator.unfold(0) {
        case i if i < n => {
          val b = (i & 0xff).toByte
          val chunk = Array.fill(i)(b)
          Some((i + 1, chunk))
        }
        case _ => None
      }
      val headers = (0 until 200).foldLeft(Seq.empty[(String, String)]) {
        case (s, i) => s :+ ("X-$i" -> i.toString)
      }
      checkSerialization(Results.Ok.stream(chunks).withHeaders(headers: _*))
    }
  }
}
