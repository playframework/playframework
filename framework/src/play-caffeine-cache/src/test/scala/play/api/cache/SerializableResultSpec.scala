/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache

import play.api.mvc.{ Result, Results }
import play.api.test._

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
      r1.header.status must_== r2.header.status
      r1.header.headers must_== r2.header.headers
      r1.body must_== r2.body
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
      checkSerialization(Results.Ok("hello!").withHeaders(CONTENT_TYPE -> "text/banana"))
      checkSerialization(Results.Ok("hello!").withHeaders(CONTENT_TYPE -> "text/banana", "X-Foo" -> "bar"))
    }
  }
}
