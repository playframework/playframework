package play.api.i18n

import org.specs2.mutable._

object MessagesSpec extends Specification {
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
  val api = new MessagesApi(testMessages)

  def translate(msg: String, lang: String, reg: String): Option[String] =
    api.translate(msg, Nil)(Lang(lang, reg))

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
  }
}
