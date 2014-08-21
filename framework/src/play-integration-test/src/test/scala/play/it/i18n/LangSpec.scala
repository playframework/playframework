/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.i18n

import play.api.i18n.Lang
import play.api.test._

class LangSpec extends PlaySpecification {
  "lang spec" should {
    "allow selecting preferred language" in {
      implicit val app = FakeApplication(additionalConfiguration = Map("application.langs" -> "en-US,es-ES,de"))
      val esEs = Lang("es", "ES")
      val es = Lang("es")
      val deDe = Lang("de", "DE")
      val de = Lang("de")
      val enUs = Lang("en", "US")

      "with exact match" in {
        Lang.preferred(Seq(esEs)) must_== esEs
      }

      "with just language match" in {
        Lang.preferred(Seq(de)) must_== de
      }

      "with just language match country specific" in {
        Lang.preferred(Seq(es)) must_== esEs
      }

      "with language and country not match just language" in {
        Lang.preferred(Seq(deDe)) must_== enUs
      }

      "with case insensitive match" in {
        Lang.preferred(Seq(Lang("ES", "es"))) must_== esEs
      }

      "in order" in {
        Lang.preferred(Seq(esEs, enUs)) must_== esEs
      }
    }

    "normalize before comparsion" in {
      Lang.get("en-us") must_== Lang.get("en-US")
      Lang.get("EN-us") must_== Lang.get("en-US")
      Lang.get("ES-419") must_== Lang.get("es-419")
      Lang.get("en-us").hashCode must_== Lang.get("en-US").hashCode
      Lang("en-us").code must_== "en-US"
      Lang("EN-us").code must_== "en-US"
      Lang("EN").code must_== "en"

      "even with locales with different caseness" in trLocaleContext {
        Lang.get("ii-ii") must_== Lang.get("ii-II")
      }

    }

    "forbid instantiation of lanague code" in {

      "with wrong format" in {
        Lang.get("en-UUS") must_== None
        Lang.get("een-US") must_== None
        Lang.get("en_US") must_== None
      }

      "with extraneous characters" in {
        Lang.get("en-ÚS") must_== None
      }

    }

  }
}

object trLocaleContext extends org.specs2.mutable.Around {
  def around[T: org.specs2.execute.AsResult](t: => T) = {
    val defaultLocale = java.util.Locale.getDefault
    java.util.Locale.setDefault(new java.util.Locale("tr"))
    val result = org.specs2.execute.AsResult(t)
    java.util.Locale.setDefault(defaultLocale)
    result
  }
}

