/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.i18n

import play.api.i18n.{ Lang, Langs }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._

class LangSpec extends PlaySpecification {
  "lang spec" should {
    "allow selecting preferred language" in {
      val esEs = Lang("es-ES")
      val es = Lang("es")
      val deDe = Lang("de-DE")
      val de = Lang("de")
      val enUs = Lang("en-US")

      implicit val app = GuiceApplicationBuilder().configure("play.i18n.langs" -> Seq(enUs, esEs, de).map(_.code)).build()
      val langs = app.injector.instanceOf[Langs]

      "with exact match" in {
        langs.preferred(Seq(esEs)) must_== esEs
      }

      "with just language match" in {
        langs.preferred(Seq(de)) must_== de
      }

      "with just language match country specific" in {
        langs.preferred(Seq(es)) must_== esEs
      }

      "with language and country not match just language" in {
        langs.preferred(Seq(deDe)) must_== enUs
      }

      "with case insensitive match" in {
        langs.preferred(Seq(Lang("ES-es"))) must_== esEs
      }

      "in order" in {
        langs.preferred(Seq(esEs, enUs)) must_== esEs
      }
    }

    "normalize before comparison" in {
      Lang.get("en-us") must_== Lang.get("en-US")
      Lang.get("EN-us") must_== Lang.get("en-US")
      Lang.get("ES-419") must_== Lang.get("es-419")
      Lang.get("en-us").hashCode must_== Lang.get("en-US").hashCode
      Lang("zh-hans").code must_== "zh-Hans"
      Lang("ZH-hant").code must_== "zh-Hant"
      Lang("en-us").code must_== "en-US"
      Lang("EN-us").code must_== "en-US"
      Lang("EN").code must_== "en"

      "even with locales with different caseness" in trLocaleContext {
        Lang.get("ii-ii") must_== Lang.get("ii-II")
      }

    }

    "forbid instantiation of language code" in {

      "with wrong format" in {
        Lang.get("e_US") must_== None
        Lang.get("en_US") must_== None
      }

      "with extraneous characters" in {
        Lang.get("en-ÚS") must_== None
      }
    }

    "allow alpha-3/ISO 639-2 language codes" in {
      "Lang instance" in {
        Lang("crh").code must_== "crh"
        Lang("ber-DZ").code must_== "ber-DZ"
      }

      "preferred language" in {
        val crhUA = Lang("crh-UA")
        val crh = Lang("crh")
        val ber = Lang("ber")
        val berDZ = Lang("ber-DZ")
        val astES = Lang("ast-ES")
        val ast = Lang("ast")

        implicit val app = GuiceApplicationBuilder().configure("play.i18n.langs" -> Seq(crhUA, ber, astES).map(_.code)).build()
        val langs = app.injector.instanceOf[Langs]

        "with exact match" in {
          langs.preferred(Seq(crhUA)) must_== crhUA
        }

        "with just language match" in {
          langs.preferred(Seq(ber)) must_== ber
        }

        "with just language match country specific" in {
          langs.preferred(Seq(ast)) must_== astES
        }

        "with language and country not match just language" in {
          langs.preferred(Seq(berDZ)) must_== crhUA
        }

        "with case insensitive match" in {
          langs.preferred(Seq(Lang("AST-es"))) must_== astES
        }

        "in order" in {
          langs.preferred(Seq(astES, crhUA)) must_== astES
        }

      }
    }

    "allow script codes" in {
      "Lang instance" in {
        Lang("zh-Hans").code must_== "zh-Hans"
        Lang("sr-Latn").code must_== "sr-Latn"
      }

      "preferred language" in {
        val enUS = Lang("en-US")
        val az = Lang("az")
        val azCyrl = Lang("az-Cyrl")
        val azLatn = Lang("az-Latn")
        val zh = Lang("zh")
        val zhHans = Lang("zh-Hans")
        val zhHant = Lang("zh-Hant")

        implicit val app = GuiceApplicationBuilder().configure("play.i18n.langs" -> Seq(zhHans, zh, azCyrl, enUS).map(_.code)).build()
        val langs = app.injector.instanceOf[Langs]

        "with exact match" in {
          langs.preferred(Seq(zhHans)) must_== zhHans
        }

        "with just language match script specific" in {
          langs.preferred(Seq(az)) must_== azCyrl
        }

        "with case insensitive match" in {
          langs.preferred(Seq(Lang("AZ-cyrl"))) must_== azCyrl
        }

        "in order" in {
          langs.preferred(Seq(azCyrl, zhHans, enUS)) must_== azCyrl
        }

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

