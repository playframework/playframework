/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.i18n

import java.io.File

import org.specs2.mutable._
import play.api.http.HttpConfiguration
import play.api.i18n.Messages.MessageSource
import play.api.mvc.{ Cookie, Results }
import play.api.{ Configuration, Environment, Mode, PlayException }
import play.core.test.FakeRequest

class MessagesSpec extends Specification {
  val testMessages = Map(
    "default" -> Map(
      "title" -> "English Title",
      "foo" -> "English foo",
      "bar" -> "English pub"),
    "fr" -> Map(
      "title" -> "Titre francais",
      "foo" -> "foo francais"),
    "fr-CH" -> Map(
      "title" -> "Titre suisse"))
  val api = {
    val env = new Environment(new File("."), this.getClass.getClassLoader, Mode.Dev)
    val config = Configuration.reference ++ Configuration.from(Map("play.i18n.langs" -> Seq("en", "fr", "fr-CH")))
    val langs = new DefaultLangsProvider(config).get
    new DefaultMessagesApi(testMessages, langs)
  }

  def translate(msg: String, lang: String, reg: String): Option[String] = {
    api.translate(msg, Nil)(Lang(lang, reg))
  }

  def isDefinedAt(msg: String, lang: String, reg: String): Boolean =
    api.isDefinedAt(msg)(Lang(lang, reg))

  "MessagesApi" should {
    "fall back to less specific translation" in {
      // Direct lookups
      translate("title", "fr", "CH") must be equalTo Some("Titre suisse")
      translate("title", "fr", "") must be equalTo Some("Titre francais")
      isDefinedAt("title", "fr", "CH") must be equalTo true
      isDefinedAt("title", "fr", "") must be equalTo true

      // Region that is missing
      translate("title", "fr", "FR") must be equalTo Some("Titre francais")
      isDefinedAt("title", "fr", "FR") must be equalTo true

      // Translation missing in the given region
      translate("foo", "fr", "CH") must be equalTo Some("foo francais")
      translate("bar", "fr", "CH") must be equalTo Some("English pub")
      isDefinedAt("foo", "fr", "CH") must be equalTo true
      isDefinedAt("bar", "fr", "CH") must be equalTo true

      // Unrecognized language
      translate("title", "bo", "GO") must be equalTo Some("English Title")
      isDefinedAt("title", "bo", "GO") must be equalTo true

      // Missing translation
      translate("garbled", "fr", "CH") must be equalTo None
      isDefinedAt("garbled", "fr", "CH") must be equalTo false
    }

    "support setting the language on a result" in {
      val cookie = api.setLang(Results.Ok, Lang("en-AU")).newCookies.head
      cookie.name must_== "PLAY_LANG"
      cookie.value must_== "en-AU"
    }

    "default for the language cookie's SameSite attribute is Lax" in {
      val env = new Environment(new File("."), this.getClass.getClassLoader, Mode.Dev)
      val config = Configuration.reference
      val langs = new DefaultLangsProvider(config).get
      val messagesApi = new DefaultMessagesApiProvider(env, config, langs, HttpConfiguration()).get
      messagesApi.langCookieSameSite must_== Option(Cookie.SameSite.Lax)
    }

    "correctly pick up the config for the language cookie's SameSite attribute" in {
      val env = new Environment(new File("."), this.getClass.getClassLoader, Mode.Dev)
      val config = Configuration.reference ++ Configuration.from(Map("play.i18n.langCookieSameSite" -> "Strict"))
      val langs = new DefaultLangsProvider(config).get
      val messagesApi = new DefaultMessagesApiProvider(env, config, langs, HttpConfiguration()).get
      messagesApi.langCookieSameSite must_== Option(Cookie.SameSite.Strict)
    }

    "not have a value for the language cookie's SameSite attribute when misconfigured" in {
      val env = new Environment(new File("."), this.getClass.getClassLoader, Mode.Dev)
      val config = Configuration.reference ++ Configuration.from(Map("play.i18n.langCookieSameSite" -> "foo"))
      val langs = new DefaultLangsProvider(config).get
      val messagesApi = new DefaultMessagesApiProvider(env, config, langs, HttpConfiguration()).get
      messagesApi.langCookieSameSite must_== None
    }

    "support getting a preferred lang from a Scala request" in {
      "when an accepted lang is available" in {
        api.preferred(FakeRequest().withHeaders("Accept-Language" -> "fr")).lang must_== Lang("fr")
      }
      "when an accepted lang is not available" in {
        api.preferred(FakeRequest().withHeaders("Accept-Language" -> "de")).lang must_== Lang("en")
      }
      "when the lang cookie available" in {
        api.preferred(FakeRequest().withCookies(Cookie("PLAY_LANG", "fr"))).lang must_== Lang("fr")
      }
      "when the lang cookie is not available" in {
        api.preferred(FakeRequest().withCookies(Cookie("PLAY_LANG", "de"))).lang must_== Lang("en")
      }
      "when a cookie and an acceptable lang are available" in {
        api.preferred(FakeRequest().withCookies(Cookie("PLAY_LANG", "fr"))
          .withHeaders("Accept-Language" -> "en")).lang must_== Lang("fr")
      }

    }

    "report error for invalid lang" in {
      {
        val langs = new DefaultLangsProvider(Configuration.reference ++ Configuration.from(Map("play.i18n.langs" -> Seq("invalid_language")))).get
        val messagesApi = new DefaultMessagesApiProvider(new Environment(new File("."), this.getClass.getClassLoader, Mode.Dev), Configuration.reference, langs, HttpConfiguration()).get
      } must throwA[PlayException]
    }
  }

  val testMessageFile = """
# this is a comment
simplekey=value
key.with.dots=value
key.with.dollar$sign=value
multiline.unix=line1\
line2
multiline.dos=line1\
line2
multiline.inline=line1\nline2
backslash.escape=\\
backslash.dummy=\a\b\c\e\f

"""

  "MessagesPlugin" should {
    "parse file" in {

      val parser = new Messages.MessagesParser(new MessageSource { def read = testMessageFile }, "messages")

      val messages = parser.parse.right.toSeq.flatten.map(x => x.key -> x.pattern).toMap

      messages("simplekey") must ===("value")
      messages("key.with.dots") must ===("value")
      messages("key.with.dollar$sign") must ===("value")
      messages("multiline.unix") must ===("line1line2")
      messages("multiline.dos") must ===("line1line2")
      messages("multiline.inline") must ===("line1\nline2")
      messages("backslash.escape") must ===("\\")
      messages("backslash.dummy") must ===("\\a\\b\\c\\e\\f")
    }
  }
}
