/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import java.io._

import com.typesafe.config.ConfigException
import org.specs2.mutable.Specification

import scala.util.control.NonFatal

object ConfigurationSpec extends Specification {

  def exampleConfig = Configuration.from(
    Map(
      "foo.bar1" -> "value1",
      "foo.bar2" -> "value2",
      "blah.0" -> List(true, false, true),
      "blah.1" -> List(1, 2, 3),
      "blah.2" -> List(1.1, 2.2, 3.3),
      "blah.3" -> List(1L, 2L, 3L),
      "blah.4" -> List("one", "two", "three"),
      "blah2" -> Map(
        "blah3" -> Map(
          "blah4" -> "value6"
        )
      )
    )
  )

  "Configuration" should {

    "be accessible as an entry set" in {
      val map = Map(exampleConfig.entrySet.toList: _*)
      map.keySet must contain(allOf("foo.bar1", "foo.bar2", "blah.0", "blah.1", "blah.2", "blah.3", "blah.4", "blah2.blah3.blah4"))
    }

    "make all paths accessible" in {
      exampleConfig.keys must contain(allOf("foo.bar1", "foo.bar2", "blah.0", "blah.1", "blah.2", "blah.3", "blah.4", "blah2.blah3.blah4"))
    }

    "make all sub keys accessible" in {
      exampleConfig.subKeys must contain(allOf("foo", "blah", "blah2"))
      exampleConfig.subKeys must not(contain(anyOf("foo.bar1", "foo.bar2", "blah.0", "blah.1", "blah.2", "blah.3", "blah.4", "blah2.blah3.blah4")))
    }

    "make all get accessible using scala" in {
      exampleConfig.getBooleanSeq("blah.0").get must ===(Seq(true, false, true))
      exampleConfig.getIntSeq("blah.1").get must ===(Seq(1, 2, 3))
      exampleConfig.getDoubleSeq("blah.2").get must ===(Seq(1.1, 2.2, 3.3))
      exampleConfig.getLongSeq("blah.3").get must ===(Seq(1L, 2L, 3L))
      exampleConfig.getStringSeq("blah.4").get must contain(exactly("one", "two", "three"))
    }

    "throw serialisable exceptions" in {
      // from Typesafe Config
      def copyViaSerialize(o: java.io.Serializable): AnyRef = {
        val byteStream = new ByteArrayOutputStream()
        val objectStream = new ObjectOutputStream(byteStream)
        objectStream.writeObject(o)
        objectStream.close()
        val inStream = new ByteArrayInputStream(byteStream.toByteArray())
        val inObjectStream = new ObjectInputStream(inStream)
        val copy = inObjectStream.readObject()
        inObjectStream.close()
        copy
      }
      val conf = Configuration.from(
        Map("item" -> "uhoh, it's gonna blow")
      );
      {
        try {
          conf.getStringList("item")
        } catch {
          case NonFatal(e) => copyViaSerialize(e)
        }
      } must not(throwA[Exception])
    }

  }

}

object PlayConfigSpec extends Specification {

  def config(data: (String, Any)*) = PlayConfig(Configuration.from(data.toMap))

  "PlayConfig" should {
    "support getting optional values" in {
      "when null" in {
        config("foo.bar" -> null).getOptional[String]("foo.bar") must beNone
      }
      "when set" in {
        config("foo.bar" -> "bar").getOptional[String]("foo.bar") must beSome("bar")
      }
      "when undefined" in {
        config().getOptional[String]("foo.bar") must throwA[ConfigException.Missing]
      }
    }
    "support getting prototyped seqs" in {
      val seq = config(
        "bars" -> Seq(Map("a" -> "different a")),
        "prototype.bars" -> Map("a" -> "some a", "b" -> "some b")
      ).getPrototypedSeq("bars")
      seq must haveSize(1)
      seq.head.get[String]("a") must_== "different a"
      seq.head.get[String]("b") must_== "some b"
    }
    "support getting prototyped maps" in {
      val map = config(
        "bars" -> Map("foo" -> Map("a" -> "different a")),
        "prototype.bars" -> Map("a" -> "some a", "b" -> "some b")
      ).getPrototypedMap("bars")
      map must haveSize(1)
      val foo = map("foo")
      foo.get[String]("a") must_== "different a"
      foo.get[String]("b") must_== "some b"
    }
  }

}
