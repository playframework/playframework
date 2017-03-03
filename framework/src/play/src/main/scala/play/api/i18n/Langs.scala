/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.i18n

import java.util.Locale
import javax.inject.{ Inject, Provider, Singleton }

import play.api.{ Application, Configuration, Logger }

import scala.util.Try
import scala.util.control.NonFatal
import scala.collection.JavaConverters._

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
   * Whether this lang satisfies the given lang.
   *
   * If the other lang defines a country code, then this is equivalent to equals, if it doesn't, then the equals is
   * only done on language and the country of this lang is ignored.
   *
   * This implements the language matching specified by RFC2616 Section 14.4.  Equality is case insensitive as per
   * Section 3.10.
   *
   * @param accept The accepted language
   */
  def satisfies(accept: Lang): Boolean =
    Locale.lookup(Seq(new Locale.LanguageRange(code)).asJava, Seq(accept.locale).asJava) != null

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
  import play.api.libs.json.{ OWrites, Reads, Writes }

  val jsonOWrites: OWrites[Lang] = implicitly[ContravariantFunctor[OWrites]].contramap[Locale, Lang](Writes.localeObjectWrites, _.locale)

  implicit val jsonTagWrites: Writes[Lang] = implicitly[ContravariantFunctor[Writes]].contramap[Locale, Lang](Writes.localeWrites, _.locale)

  val jsonOReads: Reads[Lang] = Reads.localeObjectReads.map(Lang(_))

  implicit val jsonTagReads: Reads[Lang] = Reads.localeReads.map(Lang(_))

  /**
   * The default Lang to use if nothing matches (platform default)
   */
  implicit lazy val defaultLang: Lang = Lang(java.util.Locale.getDefault)

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
    Lang(new Locale.Builder()
      .setLanguage(language)
      .setRegion(country)
      .setScript(script)
      .setVariant(variant)
      .build())

  /**
   * Create a Lang value from a code (such as fr or en-US) or none
   * if language is unrecognized.
   */
  def get(code: String): Option[Lang] = Try(apply(code)).toOption

  private val langsCache = Application.instanceCache[Langs]

  /**
   * Retrieve Lang availables from the application configuration.
   *
   * {{{
   * play.i18n.langs = ["fr", "en", "de"]
   * }}}
   */
  @deprecated("Inject Langs into your component", "2.5.0")
  def availables(implicit app: Application): Seq[Lang] = {
    langsCache(app).availables
  }

  /**
   * Guess the preferred lang in the langs set passed as argument.
   * The first Lang that matches an available Lang wins, otherwise returns the first Lang available in this application.
   */
  @deprecated("Inject Langs into your component", "2.5.0")
  def preferred(langs: Seq[Lang])(implicit app: Application): Lang = {
    langsCache(app).preferred(langs)
  }

}

/**
 * Manages languages in Play
 */
trait Langs {

  /**
   * The available languages.
   *
   * These can be configured in `application.conf`, like so:
   *
   * {{{
   * play.i18n.langs = ["fr", "en", "de"]
   * }}}
   */
  def availables: Seq[Lang]

  /**
   * Select a preferred language, given the list of candidates.
   *
   * Will select the preferred language, based on what languages are available, or return the default language if
   * none of the candidates are available.
   */
  def preferred(candidates: Seq[Lang]): Lang
}

@Singleton
class DefaultLangs @Inject() (val availables: Seq[Lang] = Seq(Lang.defaultLang)) extends Langs {
  def preferred(candidates: Seq[Lang]): Lang = candidates.collectFirst(Function.unlift { lang =>
    availables.find(_.satisfies(lang))
  }).getOrElse(availables.headOption.getOrElse(Lang.defaultLang))
}

@Singleton
class DefaultLangsProvider @Inject() (config: Configuration) extends Provider[Langs] {

  def availables: Seq[Lang] = {
    val langs = config.getOptional[String]("application.langs") map { langsStr =>
      Logger.warn("application.langs is deprecated, use play.i18n.langs instead")
      langsStr.split(",").map(_.trim).toSeq
    } getOrElse {
      config.get[Seq[String]]("play.i18n.langs")
    }

    langs.map { lang =>
      try { Lang(lang) } catch {
        case NonFatal(e) => throw config.reportError(
          "play.i18n.langs",
          "Invalid language code [" + lang + "]", Some(e))
      }
    }
  }

  lazy val get: Langs = {
    new DefaultLangs(availables)
  }
}
