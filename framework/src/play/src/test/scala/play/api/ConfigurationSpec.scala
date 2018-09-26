/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import java.io._

import com.typesafe.config.{ ConfigException, ConfigFactory }
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
      ),
      "longlong" -> 79219707376851105L,
      "longlonglist" -> Seq(-279219707376851105L, 8372206243289082062L, 1930906302765526206L)
    )
  )

  "Configuration" should {

    import scala.concurrent.duration._
    "support getting durations" in {

      "simple duration" in {
        val conf = config("my.duration" -> "10s")
        val value = conf.get[Duration]("my.duration")
        value must beEqualTo(10.seconds)
        value.toString must beEqualTo("10 seconds")
      }

      "use minutes when possible" in {
        val conf = config("my.duration" -> "120s")
        val value = conf.get[Duration]("my.duration")
        value must beEqualTo(2.minutes)
        value.toString must beEqualTo("2 minutes")
      }

      "use seconds when minutes aren't accurate enough" in {
        val conf = config("my.duration" -> "121s")
        val value = conf.get[Duration]("my.duration")
        value must beEqualTo(121.seconds)
        value.toString must beEqualTo("121 seconds")
      }

      "handle 'infinite' as Duration.Inf" in {
        val conf = config("my.duration" -> "infinite")
        conf.get[Duration]("my.duration") must beEqualTo(Duration.Inf)
      }

      "handle null as Duration.Inf" in {
        val conf = config("my.duration" -> null)
        conf.get[Duration]("my.duration") must beEqualTo(Duration.Inf)
      }

    }

    "support getting optional values via get[Option[...]]" in {
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
    "support getting optional values via getOptional" in {
      "when null" in {
        config("foo.bar" -> null).getOptional[String]("foo.bar") must beNone
      }
      "when set" in {
        config("foo.bar" -> "bar").getOptional[String]("foo.bar") must beSome("bar")
      }
      "when undefined" in {
        config().getOptional[String]("foo.bar") must beNone
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

    "handle longs of very large magnitude" in {
      exampleConfig.get[Long]("longlong") must ===(79219707376851105L)
      exampleConfig.get[Seq[Long]]("longlonglist") must ===(Seq(-279219707376851105L, 8372206243289082062L, 1930906302765526206L))
    }

    "handle invalid and null configuration values" in {
      exampleConfig.get[Seq[Boolean]]("foo.bar1") must throwA[com.typesafe.config.ConfigException]
      exampleConfig.get[Boolean]("foo.bar3") must throwA[com.typesafe.config.ConfigException]
    }

    "query maps" in {
      "objects with simple keys" in {
        val configuration = Configuration(ConfigFactory.parseString(
          """
            |foo.bar {
            |  one = 1
            |  two = 2
            |}
          """.stripMargin))

        configuration.get[Map[String, Int]]("foo.bar") must_== Map("one" -> 1, "two" -> 2)
      }
      "objects with complex keys" in {
        val configuration = Configuration(ConfigFactory.parseString(
          """
            |test.files {
            |  "/public/index.html" = "html"
            |  "/public/stylesheets/\"foo\".css" = "css"
            |  "/public/javascripts/\"bar\".js" = "js"
            |}
          """.stripMargin))
        configuration.get[Map[String, String]]("test.files") must_== Map(
          "/public/index.html" -> "html",
          """/public/stylesheets/"foo".css""" -> "css",
          """/public/javascripts/"bar".js""" -> "js"
        )
      }
      "nested objects" in {
        val configuration = Configuration(ConfigFactory.parseString(
          """
            |objects.a {
            |  "b.c" = { "D.E" = F }
            |  "d.e" = { "F.G" = H, "I.J" = K }
            |}
          """.stripMargin))
        configuration.get[Map[String, Map[String, String]]]("objects.a") must_== Map(
          "b.c" -> Map("D.E" -> "F"),
          "d.e" -> Map("F.G" -> "H", "I.J" -> "K")
        )
      }
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
