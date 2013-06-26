package play.api.i18n

import org.specs2.mutable.Specification
import play.api._

class LangSpec extends Specification {
  "lang spec" should {
    "allow selecting preferred language" in {
      implicit val app: Application = FakeApplication(Map("application.langs" -> "en-US,es-ES,de"))
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
  }
}
