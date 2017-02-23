/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.i18n

import java.util.Locale

import play.api.libs.json.{ Json, JsString, JsSuccess }

class LangSpec extends org.specs2.mutable.Specification {
  "Lang" title

  "Lang" should {
    val frLang = Lang(Locale.FRANCE)

    "be written as JSON object (default)" in {
      Json.toJson(frLang) must_== Json.obj(
        "language" -> "fr", "country" -> "FR", "variant" -> ""
      )
    }

    "be written as JSON string (short format)" in {
      Json.toJson(frLang)(Lang.jsonLocaleWrites) must_== JsString("fr-FR")
    }

    "be read from JSON object (default)" in {
      Json.fromJson[Lang](Json.obj(
        "language" -> "fr", "country" -> "FR", "variant" -> ""
      )) must_== JsSuccess(frLang)
    }

    "be read from JSON string (short format)" in {
      Json.fromJson[Lang](JsString("fr-FR"))(
        Lang.jsonLocaleReads) must_== JsSuccess(frLang)
    }
  }
}
