/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import java.io._
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import java.time.temporal.TemporalAmount
import java.time.Period
import java.util.Collections
import java.util.Objects
import java.util.Properties

import scala.concurrent.duration._
import scala.util.control.NonFatal

import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import org.specs2.execute.FailureException
import org.specs2.mutable.Specification

class ConfigurationSpec extends Specification {
  import ConfigurationSpec._

  def config(data: (String, Any)*): Configuration = Configuration.from(data.toMap)

  def exampleConfig: Configuration = config(
    "foo.bar1" -> "value1",
    "foo.bar2" -> "value2",
    "foo.bar3" -> null,
    "blah.0"   -> List(true, false, true),
    "blah.1"   -> List(1, 2, 3),
    "blah.2"   -> List(1.1, 2.2, 3.3),
    "blah.3"   -> List(1L, 2L, 3L),
    "blah.4"   -> List("one", "two", "three"),
    "blah2"    -> Map(
      "blah3" -> Map(
        "blah4" -> "value6"
      )
    ),
    "longlong"     -> 79219707376851105L,
    "longlonglist" -> Seq(-279219707376851105L, 8372206243289082062L, 1930906302765526206L),
  )

  def load(mode: Mode): Configuration = {
    // system classloader should not have an application.conf
    Configuration.load(Environment(new File("."), ClassLoader.getSystemClassLoader, mode))
  }

  "Configuration" should {
    "support getting durations" in {
      "simple duration" in {
        val conf  = config("my.duration" -> "10s")
        val value = conf.get[Duration]("my.duration")
        value must beEqualTo(10.seconds)
        value.toString must beEqualTo("10 seconds")
      }

      "use minutes when possible" in {
        val conf  = config("my.duration" -> "120s")
        val value = conf.get[Duration]("my.duration")
        value must beEqualTo(2.minutes)
        value.toString must beEqualTo("2 minutes")
      }

      "use seconds when minutes aren't accurate enough" in {
        val conf  = config("my.duration" -> "121s")
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

    "support getting java durations" in {
      "simple duration" in {
        val conf  = config("my.duration" -> "10s")
        val value = conf.get[java.time.Duration]("my.duration")
        value must beEqualTo(java.time.Duration.ofSeconds(10))
      }
    }

    "support getting periods" in {
      "month units" in {
        val conf  = config("my.period" -> "10 m")
        val value = conf.get[Period]("my.period")
        value must beEqualTo(Period.ofMonths(10))
        value.toString must beEqualTo("P10M")
      }

      "day units" in {
        val conf  = config("my.period" -> "28 days")
        val value = conf.get[Period]("my.period")
        value must beEqualTo(Period.ofDays(28))
        value.toString must beEqualTo("P28D")
      }

      "invalid format" in {
        val conf = config("my.period" -> "5 donkeys")
        conf.get[Period]("my.period") must throwA[ConfigException.BadValue]
      }
    }

    "support getting temporal amounts" in {
      "duration units" in {
        val conf  = config("my.time" -> "120s")
        val value = conf.get[TemporalAmount]("my.time")
        value must beEqualTo(java.time.Duration.ofMinutes(2))
        value.toString must beEqualTo("PT2M")
      }

      "period units" in {
        val conf  = config("my.time" -> "3 weeks")
        val value = conf.get[TemporalAmount]("my.time")
        value must beEqualTo(Period.ofWeeks(3))
        value.toString must beEqualTo("P21D")
      }

      "m means minutes, not months" in {
        val conf  = config("my.time" -> "12 m")
        val value = conf.get[TemporalAmount]("my.time")
        value must beEqualTo(java.time.Duration.ofMinutes(12))
        value.toString must beEqualTo("PT12M")
      }

      "reject 'infinite'" in {
        val conf = config("my.time" -> "infinite")
        conf.get[TemporalAmount]("my.time") must throwA[ConfigException.BadValue]
      }

      "reject `null`" in {
        val conf = config("my.time" -> null)
        conf.get[TemporalAmount]("my.time") must throwA[ConfigException.Null]
      }
    }

    "support getting URLs" in {
      val validUrl   = "https://example.com"
      val invalidUrl = "invalid-url"

      "valid URL" in {
        val conf  = config("my.url" -> validUrl)
        val value = conf.get[URL]("my.url")
        value must beEqualTo(new URI(validUrl).toURL)
      }

      "invalid URL" in {
        val conf       = config("my.url" -> invalidUrl)
        def a: Nothing = {
          conf.get[URL]("my.url")
          throw FailureException(failure("IllegalArgumentException should be thrown"))
        }
        theBlock(a) must throwA[IllegalArgumentException]
      }
    }

    "support getting URIs" in {
      val validUri   = "https://example.com"
      val invalidUri = "%"

      "valid URI" in {
        val conf  = config("my.uri" -> validUri)
        val value = conf.get[URI]("my.uri")
        value must beEqualTo(new URI(validUri))
      }

      "invalid URI" in {
        val conf       = config("my.uri" -> invalidUri)
        def a: Nothing = {
          conf.get[URI]("my.uri")
          throw FailureException(failure("URISyntaxException should be thrown"))
        }
        theBlock(a) must throwA[URISyntaxException]
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
        "bars"           -> Seq(Map("a" -> "different a")),
        "prototype.bars" -> Map("a" -> "some a", "b" -> "some b")
      ).getPrototypedSeq("bars")
      seq must haveSize(1)
      seq.head.get[String]("a") must_== "different a"
      seq.head.get[String]("b") must_== "some b"
    }

    "support getting prototyped maps" in {
      val map = config(
        "bars"           -> Map("foo" -> Map("a" -> "different a")),
        "prototype.bars" -> Map("a" -> "some a", "b" -> "some b")
      ).getPrototypedMap("bars")
      map must haveSize(1)
      val foo = map("foo")
      foo.get[String]("a") must_== "different a"
      foo.get[String]("b") must_== "some b"
    }

    "be accessible as an entry set" in {
      val map = Map(exampleConfig.entrySet.toList: _*)
      map.keySet must contain(
        allOf("foo.bar1", "foo.bar2", "blah.0", "blah.1", "blah.2", "blah.3", "blah.4", "blah2.blah3.blah4")
      )
    }

    "make all paths accessible" in {
      exampleConfig.keys must contain(
        allOf("foo.bar1", "foo.bar2", "blah.0", "blah.1", "blah.2", "blah.3", "blah.4", "blah2.blah3.blah4")
      )
    }

    "make all sub keys accessible" in {
      exampleConfig.subKeys must contain(allOf("foo", "blah", "blah2"))
      exampleConfig.subKeys must not(
        contain(anyOf("foo.bar1", "foo.bar2", "blah.0", "blah.1", "blah.2", "blah.3", "blah.4", "blah2.blah3.blah4"))
      )
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
      exampleConfig.get[Seq[Long]]("longlonglist") must ===(
        Seq(-279219707376851105L, 8372206243289082062L, 1930906302765526206L)
      )
    }

    "handle invalid and null configuration values" in {
      exampleConfig.get[Seq[Boolean]]("foo.bar1") must throwA[com.typesafe.config.ConfigException]
      exampleConfig.get[Boolean]("foo.bar3") must throwA[com.typesafe.config.ConfigException]
    }

    "query maps" in {
      "objects with simple keys" in {
        val configuration = Configuration(ConfigFactory.parseString("""
                                                                      |foo.bar {
                                                                      |  one = 1
                                                                      |  two = 2
                                                                      |}
          """.stripMargin))

        configuration.get[Map[String, Int]]("foo.bar") must_== Map("one" -> 1, "two" -> 2)
      }
      "objects with complex keys" in {
        val configuration = Configuration(ConfigFactory.parseString("""
                                                                      |test.files {
                                                                      |  "/public/index.html" = "html"
                                                                      |  "/public/stylesheets/\"foo\".css" = "css"
                                                                      |  "/public/javascripts/\"bar\".js" = "js"
                                                                      |}
          """.stripMargin))
        configuration.get[Map[String, String]]("test.files") must_== Map(
          "/public/index.html"                -> "html",
          """/public/stylesheets/"foo".css""" -> "css",
          """/public/javascripts/"bar".js"""  -> "js"
        )
      }
      "nested objects" in {
        val configuration = Configuration(ConfigFactory.parseString("""
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
        val byteStream   = new ByteArrayOutputStream()
        val objectStream = new ObjectOutputStream(byteStream)
        objectStream.writeObject(o)
        objectStream.close()
        val inStream       = new ByteArrayInputStream(byteStream.toByteArray)
        val inObjectStream = new ObjectInputStream(inStream)
        val copy           = inObjectStream.readObject()
        inObjectStream.close()
        copy
      }
      val conf = Configuration.from(
        Map("item" -> "uh-oh, it's gonna blow")
      )
      locally {
        try {
          conf.get[Seq[String]]("item")
        } catch {
          case NonFatal(e) => copyViaSerialize(e)
        }
      } must not(throwA[Exception])
    }

    "fail if application.conf is not found" in {
      "in dev mode" in (load(Mode.Dev) must throwA[PlayException])
      "in prod mode" in (load(Mode.Prod) must throwA[PlayException])
      "but not in test mode" in (load(Mode.Test) must not(throwA[PlayException]))
    }

    "throw a useful exception when invalid collections are passed in the load method" in {
      Configuration.load(Environment.simple(), Map("foo" -> Seq("one", "two"))) must throwA[PlayException]
    }

    "InMemoryResourceClassLoader should return one resource" in {
      import scala.jdk.CollectionConverters._
      val cl  = new InMemoryResourceClassLoader("reference.conf" -> "foo = ${bar}")
      val url = new URL(null, "bytes:///reference.conf", (_: URL) => throw new IOException)

      cl.findResource("reference.conf") must_== url
      cl.getResource("reference.conf") must_== url
      cl.getResources("reference.conf").asScala.toList must_== List(url)
    }

    "direct settings should have precedence over system properties when reading config.resource and config.file" in {
      val userProps = new Properties()
      userProps.put("config.resource", "application.from-user-props.res.conf")
      userProps.put("config.file", "application.from-user-props.file.conf")

      val direct = Map(
        "config.resource" -> "application.from-direct.res.conf",
        "config.file"     -> "application.from-direct.file.conf",
      )

      val cl = new InMemoryResourceClassLoader(
        "application.from-user-props.res.conf" -> "src = user-props",
        "application.from-direct.res.conf"     -> "src = direct",
      )

      val conf = Configuration.load(cl, userProps, direct, allowMissingApplicationConf = false)
      conf.get[String]("src") must_== "direct"
    }

    "load from system properties when config.resource is not defined in direct settings" in {
      val userProps = new Properties()
      userProps.put("config.resource", "application.from-user-props.res.conf")

      // Does not define config.resource nor config.file
      val direct: Map[String, AnyRef] = Map.empty

      val cl = new InMemoryResourceClassLoader(
        "application.from-user-props.res.conf" -> "src = user-props"
      )

      val conf = Configuration.load(cl, userProps, direct, allowMissingApplicationConf = false)
      conf.get[String]("src") must_== "user-props"
    }

    "validates reference.conf is self-contained" in {
      val cl = new InMemoryResourceClassLoader("reference.conf" -> "foo = ${bar}")
      Configuration.load(cl, new Properties(), Map.empty, true) must
        throwA[PlayException]("Could not resolve substitution in reference.conf to a value")
    }

    "reference values from system properties" in {
      val configuration = Configuration.load(Environment(new File("."), ClassLoader.getSystemClassLoader, Mode.Test))

      val javaVersion       = System.getProperty("java.specification.version")
      val configJavaVersion = configuration.get[String]("test.system.property.java.spec.version")

      configJavaVersion must beEqualTo(javaVersion)
    }

    "reference values from system properties when passing additional properties" in {
      val configuration = Configuration.load(
        ClassLoader.getSystemClassLoader,
        new Properties(), // empty so that we can check that System Properties are still considered
        directSettings = Map.empty,
        allowMissingApplicationConf = true
      )

      val javaVersion       = System.getProperty("java.specification.version")
      val configJavaVersion = configuration.get[String]("test.system.property.java.spec.version")

      configJavaVersion must beEqualTo(javaVersion)
    }

    "system properties override user-defined properties" in {
      val userProperties = new Properties()
      userProperties.setProperty("java.specification.version", "my java version")

      val configuration = Configuration.load(
        ClassLoader.getSystemClassLoader,
        userProperties,
        directSettings = Map.empty,
        allowMissingApplicationConf = true
      )

      val javaVersion       = System.getProperty("java.specification.version")
      val configJavaVersion = configuration.get[String]("test.system.property.java.spec.version")

      configJavaVersion must beEqualTo(javaVersion)
    }
  }
}

object ConfigurationSpec {

  /** Allows loading in-memory resources. */
  final class InMemoryResourceClassLoader(entries: (String, String)*) extends ClassLoader {
    val bytes = entries.toMap.view.mapValues(_.getBytes(StandardCharsets.UTF_8)).toMap

    override def findResource(name: String) = {
      Objects.requireNonNull(name)
      val spec = s"bytes:///$name"
      bytes.get(name) match {
        case None        => null
        case Some(bytes) => new URL(null, spec, (url: URL) => new BytesUrlConnection(url, bytes))
      }
    }

    override def getResource(name: String) = findResource(name)

    override def getResources(name: String) = {
      findResource(name) match {
        case null => Collections.emptyEnumeration()
        case res1 => Collections.enumeration(Collections.singleton(res1))
      }
    }
  }

  final class BytesUrlConnection(url: URL, bytes: Array[Byte]) extends URLConnection(url) {
    def connect()                            = ()
    override def getInputStream: InputStream = new ByteArrayInputStream(bytes)
  }
}
