/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.i18n

import java.util.Locale
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import play.api.Configuration
import play.api.Logger

import scala.util.Try
import scala.util.control.NonFatal
import scala.jdk.CollectionConverters._

/**
 * A Lang supported by the application.
 */
case class Lang(locale: Locale) {

  /**
   * Convert to a Java Locale value.
   */
  def toLocale: Locale = locale

  /**
   * @return The language for this Lang.
   */
  def language: String = locale.getLanguage

  /**
   * @return The country for this Lang, or "" if none exists.
   */
  def country: String = locale.getCountry

  /**
   * @return The script tag for this Lang, or "" if none exists.
   */
  def script: String = locale.getScript

  /**
   * @return The variant tag for this Lang, or "" if none exists.
   */
  def variant: String = locale.getVariant

  /**
   * The language tag (such as fr or en-US).
   */
  lazy val code: String = locale.toLanguageTag
}

/**
 * Utilities related to Lang values.
 */
object Lang {
  import play.api.libs.functional.ContravariantFunctor
  import play.api.libs.json.OWrites
  import play.api.libs.json.Reads
  import play.api.libs.json.Writes

  val jsonOWrites: OWrites[Lang] =
    implicitly[ContravariantFunctor[OWrites]]
      .contramap[Locale, Lang](Writes.localeObjectWrites, _.locale)

  implicit val jsonTagWrites: Writes[Lang] =
    implicitly[ContravariantFunctor[Writes]]
      .contramap[Locale, Lang](Writes.localeWrites, _.locale)

  val jsonOReads: Reads[Lang] = Reads.localeObjectReads.map(Lang(_))

  implicit val jsonTagReads: Reads[Lang] = Reads.localeReads.map(Lang(_))

  /**
   * The default Lang to use if nothing matches (platform default).
   *
   * Pre 2.6.x, defaultLang was an implicit value, meaning that it could be used in implicit scope
   * resolution if no Lang was found in local scope.  This setting was too general and resulted
   * in bugs where the defaultLang was being used instead of a request.lang, if request was not
   * declared as implicit.
   */
  lazy val defaultLang: Lang = Lang(java.util.Locale.getDefault)

  /**
   * Create a Lang value from a code (such as fr or en-US) and
   *  throw exception if language is unrecognized
   */
  def apply(code: String): Lang =
    Lang(new Locale.Builder().setLanguageTag(code).build())

  /**
   * Create a Lang value from a code (such as fr or en-US) and
   *  throw exception if language is unrecognized
   */
  def apply(language: String, country: String = "", script: String = "", variant: String = ""): Lang =
    Lang(
      new Locale.Builder()
        .setLanguage(language)
        .setRegion(country)
        .setScript(script)
        .setVariant(variant)
        .build()
    )

  /**
   * Create a Lang value from a code (such as fr or en-US) or none
   * if language is unrecognized.
   */
  def get(code: String): Option[Lang] = Try(apply(code)).toOption

  val logger = Logger(getClass)
}
