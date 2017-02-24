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

    "be written as JSON object" in {
      Json.toJson(frLang)(Lang.jsonOWrites) must_== Json.obj(
        "language" -> "fr", "country" -> "FR", "variant" -> ""
      )
    }

    "be written as JSON string (tag)" in {
      Json.toJson(frLang) must_== JsString("fr-FR")
    }

    "be read from JSON object" in {
      Json.fromJson[Lang](Json.obj(
        "language" -> "fr", "country" -> "FR", "variant" -> ""
      ))(Lang.jsonOReads) must_== JsSuccess(frLang)
    }

    "be read from JSON string (tag)" in {
      Json.fromJson[Lang](JsString("fr-FR")) must_== JsSuccess(frLang)
    }
  }
}
