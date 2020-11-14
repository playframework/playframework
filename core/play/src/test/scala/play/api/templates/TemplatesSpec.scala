/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.templates

import java.util.Optional

import akka.util.ByteString
import org.specs2.mutable._
import play.api.Configuration
import play.api.Environment
import play.api.http.HttpConfiguration
import play.api.http.HttpEntity
import play.api.http.Writeable
import play.api.i18n.DefaultLangsProvider
import play.api.i18n.DefaultMessagesApiProvider
import play.api.i18n.Messages
import play.api.mvc.Results
import play.mvc.{ Results => JResults }
import play.twirl.api.Html

import scala.collection.JavaConverters._

class TemplatesSpec extends Specification {
  "toHtmlArgs" should {
    "escape attribute values" in {
      PlayMagic.toHtmlArgs(Map('foo -> """bar <>&"'""")).body must_== """foo="bar &lt;&gt;&amp;&quot;&#x27;""""
    }
  }

  "translate" should {
    val conf                        = Configuration.reference
    val langs                       = new DefaultLangsProvider(conf).get
    val httpConfiguration           = HttpConfiguration.fromConfiguration(conf, Environment.simple())
    val messagesApi                 = new DefaultMessagesApiProvider(Environment.simple(), conf, langs, httpConfiguration).get
    implicit val messages: Messages = messagesApi.preferred(Seq.empty)

    "handle String" in {
      PlayMagic.translate("myfieldlabel") must_== """I am the <b>label</b> of the field"""
    }

    "handle Html" in {
      PlayMagic.translate(Html("myfieldlabel")) must_== Html("""I am the <b>label</b> of the field""")
    }

    "handle Option[String]" in {
      PlayMagic.translate(Some("myfieldlabel")) must_== Some("""I am the <b>label</b> of the field""")
    }

    "handle Option[Html]" in {
      PlayMagic.translate(Some(Html("myfieldlabel"))) must_== Some(Html("""I am the <b>label</b> of the field"""))
    }

    "handle Optional[String]" in {
      PlayMagic.translate(Optional.of("myfieldlabel")) must_== Optional.of("""I am the <b>label</b> of the field""")
    }

    "handle Optional[Html]" in {
      PlayMagic.translate(Optional.of(Html("myfieldlabel"))) must_== Optional.of(
        Html("""I am the <b>label</b> of the field""")
      )
    }

    "handle Seq[String, Html, Option[String], Option[Html]]" in {
      PlayMagic.translate(
        Seq("myfieldlabel", Html("myfieldname"), Some("myfieldlabel"), Some(Html("myfieldname")))
      ) must_== Seq(
        """I am the <b>label</b> of the field""",
        Html("""I am the <b>name</b> of the field"""),
        Some("""I am the <b>label</b> of the field"""),
        Some(Html("""I am the <b>name</b> of the field"""))
      )
    }

    "handle Java List[String, Html, Optional[String], Optional[Html]]" in {
      PlayMagic.translate(
        Seq("myfieldlabel", Html("myfieldname"), Optional.of("myfieldlabel"), Optional.of(Html("myfieldname"))).asJava
      ) must_== Seq(
        """I am the <b>label</b> of the field""",
        Html("""I am the <b>name</b> of the field"""),
        Optional.of("""I am the <b>label</b> of the field"""),
        Optional.of(Html("""I am the <b>name</b> of the field"""))
      ).asJava
    }

    "handle array" in {
      PlayMagic.translate(
        Array("myfieldlabel", Html("myfieldname"), Optional.of("myfieldlabel"), Optional.of(Html("myfieldname")))
      ) must_== Array(
        """I am the <b>label</b> of the field""",
        Html("""I am the <b>name</b> of the field"""),
        Optional.of("""I am the <b>label</b> of the field"""),
        Optional.of(Html("""I am the <b>name</b> of the field"""))
      )
    }

    "handle String that can't be found in messages" in {
      PlayMagic.translate("foo.me") must_== "foo.me"
    }

    "handle Html that can't be found in messages" in {
      PlayMagic.translate(Html("foo.me")) must_== Html("foo.me")
    }

    "handle String that can't be found in messages wrapped in Option" in {
      PlayMagic.translate(Some("foo.me")) must_== Some("foo.me")
    }

    "handle Html that can't be found in messages wrapped in Option" in {
      PlayMagic.translate(Some(Html("foo.me"))) must_== Some(Html("foo.me"))
    }

    "handle String that can't be found in messages wrapped in Optional" in {
      PlayMagic.translate(Optional.of("foo.me")) must_== Optional.of("foo.me")
    }

    "handle Html that can't be found in messages wrapped in Optional" in {
      PlayMagic.translate(Optional.of(Html("foo.me"))) must_== Optional.of(Html("foo.me"))
    }

    "handle non String / non Html" in {
      PlayMagic.translate(4) must_== 4
    }

    "handle non String / non Html wrapped in Option" in {
      PlayMagic.translate(Some(4)) must_== Some(4)
    }

    "handle non String / non Html wrapped in Optional" in {
      PlayMagic.translate(Optional.of(4)) must_== Optional.of(4)
    }
  }

  "Xml" should {
    import play.twirl.api.Xml

    val xml = Xml("\n\t xml")

    "have body trimmed by implicit Writeable" in {
      val writeable = implicitly[Writeable[Xml]]
      string(writeable.transform(xml)) must_== "xml"
    }

    "have Scala result body trimmed" in {
      consume(Results.Ok(xml).body) must_== "xml"
    }

    "have Java result body trimmed" in {
      consume(JResults.ok(xml).asScala.body) must_== "xml"
    }
  }

  def string(bytes: ByteString): String = bytes.utf8String

  def consume(entity: HttpEntity): String = entity match {
    case HttpEntity.Strict(data, _) => string(data)
    case _                          => throw new IllegalArgumentException("Expected strict body")
  }
}
