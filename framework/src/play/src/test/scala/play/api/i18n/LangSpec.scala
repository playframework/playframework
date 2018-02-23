/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.i18n

import java.util.Locale

import play.api.libs.json.{ Json, JsString, JsSuccess }

import org.specs2.specification.core.Fragments

class LangSpec extends org.specs2.mutable.Specification {
  "Lang" title

  "Lang" should {
    def fullLocale = new Locale.Builder().setLocale(Locale.FRANCE).
      addUnicodeLocaleAttribute("foo").addUnicodeLocaleAttribute("bar").
      setExtension('a', "foo").setExtension('b', "bar").
      setRegion("FR").setScript("Latn").setVariant("polyton").
      setUnicodeLocaleKeyword("ka", "ipsum").
      setUnicodeLocaleKeyword("kb", "value").
      build()

    val locales = Seq(
      Locale.FRANCE, Locale.CANADA_FRENCH, new Locale("fr"), fullLocale)

    val tags = Seq("fr-FR", "fr-CA", "fr",
      "fr-Latn-FR-polyton-a-foo-b-bar-u-bar-foo-ka-ipsum-kb-value")

    val objs = Seq(
      Json.obj("language" -> "fr", "country" -> "FR"),
      Json.obj("language" -> "fr", "country" -> "CA"),
      Json.obj("language" -> "fr"),
      Json.obj("variant" -> "polyton", "country" -> "FR",
        "attributes" -> Json.arr("bar", "foo"), "language" -> "fr",
        "keywords" -> Json.obj("ka" -> "ipsum", "kb" -> "value"),
        "script" -> "Latn", "extension" -> Json.obj(
          "a" -> "foo", "b" -> "bar", "u" -> "bar-foo-ka-ipsum-kb-value"
        )
      )
    )

    Fragments.foreach(locales zip objs) {
      case (locale, obj) =>
        s"be ${locale.toLanguageTag}" >> {
          "and written as JSON object" in {
            Json.toJson(Lang(locale))(Lang.jsonOWrites) must_== obj
          }

          "be read as JSON object" in {
            Json.fromJson[Lang](obj)(Lang.jsonOReads) mustEqual (
              JsSuccess(Lang(locale)))
          }
        }
    }

    Fragments.foreach(locales zip tags) {
      case (locale, tag) =>
        s"be ${locale.toLanguageTag}" >> {
          "and written as JSON string (tag)" in {
            Json.toJson(Lang(locale)) must_== JsString(tag)
          }

          "be read from JSON string (tag)" in {
            Json.fromJson[Lang](JsString(tag)) must_== JsSuccess(Lang(locale))
          }
        }
    }
  }
}
