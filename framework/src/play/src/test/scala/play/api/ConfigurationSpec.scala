/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api

import java.io._

import com.typesafe.config.ConfigException
import org.specs2.mutable.Specification

import scala.util.control.NonFatal

class ConfigurationSpec extends Specification {

  def config(data: (String, Any)*) = Configuration.from(data.toMap)

  def exampleConfig = Configuration.from(
    Map(
      "foo.bar1" -> "value1",
      "foo.bar2" -> "value2",
      "foo.bar3" -> null,
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

    "support getting optional values" in {
      "when null" in {
        config("foo.bar" -> null).get[Option[String]]("foo.bar") must beNone
      }
      "when set" in {
        config("foo.bar" -> "bar").get[Option[String]]("foo.bar") must beSome("bar")
      }
      "when undefined" in {
        config().get[Option[String]]("foo.bar") must throwA[ConfigException.Missing]
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
      exampleConfig.get[Seq[Boolean]]("blah.0") must ===(Seq(true, false, true))
      exampleConfig.get[Seq[Int]]("blah.1") must ===(Seq(1, 2, 3))
      exampleConfig.get[Seq[Double]]("blah.2") must ===(Seq(1.1, 2.2, 3.3))
      exampleConfig.get[Seq[Long]]("blah.3") must ===(Seq(1L, 2L, 3L))
      exampleConfig.get[Seq[String]]("blah.4") must contain(exactly("one", "two", "three"))
    }

    "handle invalid and null configuration values" in {
      exampleConfig.get[Seq[Boolean]]("foo.bar1") must throwA[com.typesafe.config.ConfigException]
      exampleConfig.get[Boolean]("foo.bar3") must throwA[com.typesafe.config.ConfigException]
    }

    "throw serializable exceptions" in {
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
          conf.get[Seq[String]]("item")
        } catch {
          case NonFatal(e) => copyViaSerialize(e)
        }
      } must not(throwA[Exception])
    }

    "fail if application.conf is not found" in {
      def load(mode: Mode) = {
        // system classloader should not have an application.conf
        Configuration.load(Environment(new File("."), ClassLoader.getSystemClassLoader, mode))
      }
      "in dev mode" in {
        load(Mode.Dev) must throwA[PlayException]
      }
      "in prod mode" in {
        load(Mode.Prod) must throwA[PlayException]
      }
      "but not in test mode" in {
        load(Mode.Test) must not(throwA[PlayException])
      }
    }
    "throw a useful exception when invalid collections are passed in the load method" in {
      Configuration.load(Environment.simple(), Map("foo" -> Seq("one", "two"))) must throwA[PlayException]
    }
  }

}
