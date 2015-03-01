/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

import java.util.UUID
import org.specs2.mutable._

object BindersSpec extends Specification {

  val uuid = UUID.randomUUID

  "UUID path binder" should {
    val subject = implicitly[PathBindable[UUID]]

    "Unbind UUID as string" in {
      subject.unbind("key", uuid) must be_==(uuid.toString)
    }
    "Bind parameter to UUID" in {
      subject.bind("key", uuid.toString) must be_==(Right(uuid))
    }
    "Fail on unparseable UUID" in {
      subject.bind("key", "bad-uuid") must be_==(Left("Cannot parse parameter key as UUID: Invalid UUID string: bad-uuid"))
    }
  }

  "UUID query string binder" should {
    val subject = implicitly[QueryStringBindable[UUID]]

    "Unbind UUID as string" in {
      subject.unbind("key", uuid) must be_==("key=" + uuid.toString)
    }
    "Bind parameter to UUID" in {
      subject.bind("key", Map("key" -> Seq(uuid.toString))) must be_==(Some(Right(uuid)))
    }
    "Fail on unparseable UUID" in {
      subject.bind("key", Map("key" -> Seq("bad-uuid"))) must be_==(Some(Left("Cannot parse parameter key as UUID: Invalid UUID string: bad-uuid")))
    }
  }

  "URL Path string binder" should {
    val subject = implicitly[PathBindable[String]]
    val pathString = "/path/to/some%20file"
    val pathStringBinded = "/path/to/some file"

    "Unbind Path string as string" in {
      subject.unbind("key", pathString) must equalTo(pathString)
    }
    "Bind Path string as string without any decoding" in {
      subject.bind("key", pathString) must equalTo(Right(pathString))
    }
  }

  "QueryStringBindable.bindableString" should {
    "unbind with null values" in {
      import QueryStringBindable._
      val boundValue = bindableString.unbind("key", null)
      boundValue must beEqualTo("key=")
    }
  }

  "QueryStringBindable.bindableSeq" should {
    val seqBinder = implicitly[QueryStringBindable[Seq[String]]]
    val values = Seq("i", "once", "knew", "a", "man", "from", "nantucket")
    val params = Map("q" -> values)

    "propagate errors that occur during bind" in {
      implicit val brokenBinder: QueryStringBindable[String] = {
        new QueryStringBindable.Parsing[String](
          { x =>
            if (x == "i" || x == "nantucket") x else sys.error(s"failed: ${x}")
          },
          identity,
          (key, ex) => s"failed to parse ${key}: ${ex.getMessage}"
        )
      }
      val brokenSeqBinder = implicitly[QueryStringBindable[Seq[String]]]
      val err = s"""failed to parse q: failed: once
      |failed to parse q: failed: knew
      |failed to parse q: failed: a
      |failed to parse q: failed: man
      |failed to parse q: failed: from""".stripMargin

      brokenSeqBinder.bind("q", params) must equalTo(Some(Left(err)))
    }

    "preserve the order of bound parameters" in {
      seqBinder.bind("q", params) must equalTo(Some(Right(values)))
    }

    "return the empty list when the key is not found" in {
      seqBinder.bind("q", Map.empty) must equalTo(Some(Right(Nil)))
    }
  }

}
