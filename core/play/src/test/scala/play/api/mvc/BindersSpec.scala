/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.util.UUID
import org.specs2.mutable._

case class Demo(value: Long) extends AnyVal
case class Hase(x: String)   extends AnyVal

class BindersSpec extends Specification {
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
      subject.bind("key", "bad-uuid") must be_==(
        Left("Cannot parse parameter key as UUID: Invalid UUID string: bad-uuid")
      )
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
      subject.bind("key", Map("key" -> Seq("bad-uuid"))) must be_==(
        Some(Left("Cannot parse parameter key as UUID: Invalid UUID string: bad-uuid"))
      )
    }
    "Unbind with keys needing encode" in {
      val u          = UUID.randomUUID()
      val boundValue = subject.unbind("ke=y", u)
      boundValue must beEqualTo("ke%3Dy=" + u.toString)
    }
  }

  "URL Path string binder" should {
    val subject          = implicitly[PathBindable[String]]
    val pathString       = "/path/to/some%20file"
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
    "unbind with keys and values needing encode String" in {
      import QueryStringBindable._
      val boundValue = bindableString.unbind("ke=y", "b=ar")
      boundValue must beEqualTo("ke%3Dy=b%3Dar")
    }
  }

  "QueryStringBindable.bindableSeq" should {
    val seqBinder = implicitly[QueryStringBindable[Seq[String]]]
    val values    = Seq("i", "once", "knew", "a", "man", "from", "nantucket")
    val params    = Map("q" -> values)

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
      val err             = s"""failed to parse q: failed: once
                   |failed to parse q: failed: knew
                   |failed to parse q: failed: a
                   |failed to parse q: failed: man
                   |failed to parse q: failed: from""".stripMargin.replaceAll(System.lineSeparator, "\n") // Windows compatibility

      brokenSeqBinder.bind("q", params) must equalTo(Some(Left(err)))
    }

    "preserve the order of bound parameters" in {
      seqBinder.bind("q", params) must equalTo(Some(Right(values)))
    }

    "return the empty list when the key is not found" in {
      seqBinder.bind("q", Map.empty) must equalTo(Some(Right(Nil)))
    }
  }

  "URL QueryStringBindable Char" should {
    val subject = implicitly[QueryStringBindable[Char]]
    val char    = 'X'
    val string  = "X"

    "Unbind query string char as string" in {
      subject.unbind("key", char) must equalTo("key=" + char.toString)
    }
    "Bind query string as char" in {
      subject.bind("key", Map("key" -> Seq(string))) must equalTo(Some(Right(char)))
    }
    "Fail on length > 1" in {
      subject.bind("key", Map("key" -> Seq("foo"))) must be_==(
        Some(Left("Cannot parse parameter key with value 'foo' as Char: key must be exactly one digit in length."))
      )
    }
    "Be None on empty" in {
      subject.bind("key", Map("key" -> Seq(""))) must equalTo(None)
    }
    "Unbind with keys needing encode" in {
      val boundValue = subject.unbind("ke=y", char)
      boundValue must beEqualTo("ke%3Dy=" + string)
    }
  }

  "URL QueryStringBindable Java Character" should {
    val subject         = implicitly[QueryStringBindable[Character]]
    val char: Character = 'X'
    val string          = "X"

    "Unbind query string char as string" in {
      subject.unbind("key", char) must equalTo("key=" + char.toString)
    }
    "Bind query string as char" in {
      subject.bind("key", Map("key" -> Seq(string))) must equalTo(Some(Right(char)))
    }
    "Fail on length > 1" in {
      subject.bind("key", Map("key" -> Seq("foo"))) must be_==(
        Some(Left("Cannot parse parameter key with value 'foo' as Char: key must be exactly one digit in length."))
      )
    }
    "Be None on empty" in {
      subject.bind("key", Map("key" -> Seq(""))) must equalTo(None)
    }
    "Unbind with keys needing encode" in {
      val boundValue = subject.unbind("ke=y", char)
      boundValue must beEqualTo("ke%3Dy=" + string)
    }
  }

  "URL QueryStringBindable Short" should {
    val subject = implicitly[QueryStringBindable[Short]]
    val short   = 7.toShort
    val string  = "7"

    "Unbind query string short as string" in {
      subject.unbind("key", short) must equalTo("key=" + short.toString)
    }
    "Bind query string as short" in {
      subject.bind("key", Map("key" -> Seq(string))) must equalTo(Some(Right(short)))
    }
    "Fail on value must contain only digits" in {
      subject.bind("key", Map("key" -> Seq("foo"))) must be_==(
        Some(Left("Cannot parse parameter key as Short: For input string: \"foo\""))
      )
    }
    "Fail on value < -32768" in {
      subject.bind("key", Map("key" -> Seq("-32769"))) must be_==(
        Some(Left("Cannot parse parameter key as Short: Value out of range. Value:\"-32769\" Radix:10"))
      )
    }
    "Fail on value > 32767" in {
      subject.bind("key", Map("key" -> Seq("32768"))) must be_==(
        Some(Left("Cannot parse parameter key as Short: Value out of range. Value:\"32768\" Radix:10"))
      )
    }
    "Be None on empty" in {
      subject.bind("key", Map("key" -> Seq(""))) must equalTo(None)
    }
    "Unbind with keys needing encode" in {
      val boundValue = subject.unbind("ke=y", short)
      boundValue must beEqualTo("ke%3Dy=" + string)
    }
  }

  "URL PathBindable Char" should {
    val subject = implicitly[PathBindable[Char]]
    val char    = 'X'
    val string  = "X"

    "Unbind Path char as string" in {
      subject.unbind("key", char) must equalTo(char.toString)
    }
    "Bind Path string as char" in {
      subject.bind("key", string) must equalTo(Right(char))
    }
    "Fail on length > 1" in {
      subject.bind("key", "foo") must be_==(
        Left("Cannot parse parameter key with value 'foo' as Char: key must be exactly one digit in length.")
      )
    }
    "Fail on empty" in {
      subject.bind("key", "") must be_==(
        Left("Cannot parse parameter key with value '' as Char: key must be exactly one digit in length.")
      )
    }
  }

  "URL PathBindable Java Character" should {
    val subject         = implicitly[PathBindable[Character]]
    val char: Character = 'X'
    val string          = "X"

    "Unbind Path char as string" in {
      subject.unbind("key", char) must equalTo(char.toString)
    }
    "Bind Path string as char" in {
      subject.bind("key", string) must equalTo(Right(char))
    }
    "Fail on length > 1" in {
      subject.bind("key", "foo") must be_==(
        Left("Cannot parse parameter key with value 'foo' as Char: key must be exactly one digit in length.")
      )
    }
    "Fail on empty" in {
      subject.bind("key", "") must be_==(
        Left("Cannot parse parameter key with value '' as Char: key must be exactly one digit in length.")
      )
    }
  }

  "AnyVal PathBindable" should {
    "Bind Long String as Demo" in {
      implicitly[PathBindable[Demo]].bind("key", "10") must equalTo(Right(Demo(10L)))
    }
    "Unbind Hase as String" in {
      implicitly[PathBindable[Hase]].unbind("key", Hase("Disney_Land")) must equalTo("Disney_Land")
    }
  }

  "AnyVal QueryStringBindable" should {
    "Bind Long String as Demo" in {
      implicitly[QueryStringBindable[Demo]].bind("key", Map("key" -> Seq("10"))) must equalTo(Some(Right(Demo(10L))))
    }
    "Unbind with keys needing encode (String)" in {
      val boundValue = implicitly[QueryStringBindable[Demo]].unbind("ke=y", Demo(11L))
      boundValue must beEqualTo("ke%3Dy=11")
    }
    "Unbind Hase as String" in {
      implicitly[QueryStringBindable[Hase]].unbind("key", Hase("Disney_Land")) must equalTo("key=Disney_Land")
    }
    "Unbind with keys and values needing encode (String)" in {
      val boundValue = implicitly[QueryStringBindable[Hase]].unbind("ke=y", Hase("Kre=mlin"))
      boundValue must beEqualTo("ke%3Dy=Kre%3Dmlin")
    }
  }

  "URL QueryStringBindable Int" should {
    val subject = implicitly[QueryStringBindable[Int]]
    val int     = 6182
    val string  = "6182"

    "Unbind query string int as string" in {
      subject.unbind("key", int) must equalTo(s"key=${string}")
    }
    "Bind query string as int" in {
      subject.bind("key", Map("key" -> Seq(string))) must beSome(Right(int))
    }
    "Fail on value must contain only digits" in {
      subject.bind("key", Map("key" -> Seq("foo"))) must beSome(
        Left("Cannot parse parameter key as Int: For input string: \"foo\"")
      )
    }
    "Fail on value < -2147483648" in {
      subject.bind("key", Map("key" -> Seq("-2147483649"))) must beSome(
        Left("Cannot parse parameter key as Int: For input string: \"-2147483649\"")
      )
    }
    "Fail on value > 2147483647" in {
      subject.bind("key", Map("key" -> Seq("2147483648"))) must beSome(
        Left("Cannot parse parameter key as Int: For input string: \"2147483648\"")
      )
    }
    "Be None on empty" in {
      subject.bind("key", Map("key" -> Seq(""))) must beNone
    }
  }
}
