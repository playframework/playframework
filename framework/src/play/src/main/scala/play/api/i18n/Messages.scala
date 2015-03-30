/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.i18n

import javax.inject.{ Inject, Singleton }

import play.api.inject.Module
import play.api.mvc.{ DiscardingCookie, Cookie, Result, RequestHeader }
import play.mvc.Http

import scala.language.postfixOps

import play.api._
import play.utils.{ PlayIO, Resources }
import play.Logger

import scala.util.parsing.input._
import scala.util.parsing.combinator._
import scala.util.control.NonFatal
import java.net.URL
import scala.io.Codec

/**
 * A Lang supported by the application.
 *
 * @param language a valid ISO Language Code.
 * @param country a valid ISO Country Code.
 */
case class Lang(language: String, country: String = "") {

  /**
   * Convert to a Java Locale value.
   */
  lazy val toLocale = Option(country).filterNot(_.isEmpty).map(c => new java.util.Locale(language, c)).getOrElse(new java.util.Locale(language))

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
  def satisfies(accept: Lang) = language.equalsIgnoreCase(accept.language) && (accept match {
    case Lang(_, "") => true
    case Lang(_, c) => country.equalsIgnoreCase(c)
  })

  /**
   * The Lang code (such as fr or en-US).
   */
  lazy val code = language.toLowerCase(java.util.Locale.ENGLISH) + Option(country).filterNot(_.isEmpty).map("-" + _.toUpperCase(java.util.Locale.ENGLISH)).getOrElse("")

  override def equals(that: Any) = {
    that match {
      case lang: Lang => code == lang.code
      case _ => false
    }
  }

  override def hashCode: Int = code.hashCode
}

/**
 * Utilities related to Lang values.
 */
object Lang {

  /**
   * The default Lang to use if nothing matches (platform default)
   */
  implicit lazy val defaultLang = {
    val defaultLocale = java.util.Locale.getDefault
    Lang(defaultLocale.getLanguage, defaultLocale.getCountry)
  }

  private val SimpleLocale = """([a-zA-Z]{2,3})""".r
  private val CountryLocale = (SimpleLocale.toString + """-([a-zA-Z]{2}|[0-9]{3})""").r

  /**
   * Create a Lang value from a code (such as fr or en-US) and
   *  throw exception if language is unrecognized
   */
  def apply(code: String): Lang = {
    get(code).getOrElse(
      sys.error("Unrecognized language: %s".format(code))
    )
  }

  /**
   * Create a Lang value from a code (such as fr or en-US) or none
   * if language is unrecognized.
   */
  def get(code: String): Option[Lang] = {
    code match {
      case SimpleLocale(language) => Some(Lang(language, ""))
      case CountryLocale(language, country) => Some(Lang(language, country))
      case _ => None
    }
  }

  private val langsCache = Application.instanceCache[Langs]

  /**
   * Retrieve Lang availables from the application configuration.
   *
   * {{{
   * play.i18n.langs = ["fr", "en", "de"]
   * }}}
   */
  def availables(implicit app: Application): Seq[Lang] = {
    langsCache(app).availables
  }

  /**
   * Guess the preferred lang in the langs set passed as argument.
   * The first Lang that matches an available Lang wins, otherwise returns the first Lang available in this application.
   */
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
   * play.modules.i18n.langs="fr,en,de"
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
class DefaultLangs @Inject() (configuration: Configuration) extends Langs {

  private val config = PlayConfig(configuration)
  val availables = {
    val langs = configuration.getString("application.langs") map { langsStr =>
      Logger.warn("application.langs is deprecated, use play.i18n.langs instead")
      langsStr.split(",").map(_.trim).toSeq
    } getOrElse {
      config.get[Seq[String]]("play.i18n.langs")
    }

    langs.map { lang =>
      try { Lang(lang) } catch {
        case NonFatal(e) => throw configuration.reportError("play.modules.i18n.langs",
          "Invalid language code [" + lang + "]", Some(e))
      }
    }
  }

  def preferred(candidates: Seq[Lang]) = candidates.collectFirst(Function.unlift { lang =>
    availables.find(_.satisfies(lang))
  }).getOrElse(availables.headOption.getOrElse(Lang.defaultLang))
}

/**
 * Internationalisation API.
 *
 * For example:
 * {{{
 * val msgString = Messages("items.found", items.size)
 * }}}
 */
object Messages {

  private[play] val messagesApiCache = Application.instanceCache[MessagesApi]

  /**
   * Implicit conversions providing [[Messages]] or [[MessagesApi]] using an implicit [[Application]], for a smooth upgrade to 2.4
   */
  object Implicits {
    import scala.language.implicitConversions
    implicit def applicationMessagesApi(implicit application: Application): MessagesApi =
      messagesApiCache(application)
    implicit def applicationMessages(implicit lang: Lang, application: Application): Messages =
      new Messages(lang, messagesApiCache(application))
  }

  /**
   * Translates a message.
   *
   * Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param key the message key
   * @param args the message arguments
   * @return the formatted message or a default rendering if the key wasn’t defined
   */
  def apply(key: String, args: Any*)(implicit messages: Messages): String = {
    messages(key, args: _*)
  }

  /**
   * Translates the first defined message.
   *
   * Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param keys the message key
   * @param args the message arguments
   * @return the formatted message or a default rendering if the key wasn’t defined
   */
  def apply(keys: Seq[String], args: Any*)(implicit messages: Messages): String = {
    messages(keys, args: _*)
  }

  /**
   * Check if a message key is defined.
   * @param key the message key
   * @return a boolean
   */
  def isDefinedAt(key: String)(implicit messages: Messages): Boolean = {
    messages.isDefinedAt(key)
  }

  /**
   * Parse all messages of a given input.
   */
  def parse(messageSource: MessageSource, messageSourceName: String): Either[PlayException.ExceptionSource, Map[String, String]] = {
    new Messages.MessagesParser(messageSource, "").parse.right.map { messages =>
      messages.map { message => message.key -> message.pattern }.toMap
    }
  }

  /**
   * A source for messages
   */
  trait MessageSource {
    /**
     * Read the message source as a String
     */
    def read: String
  }

  case class UrlMessageSource(url: URL) extends MessageSource {
    def read = PlayIO.readUrlAsString(url)(Codec.UTF8)
  }

  private[i18n] case class Message(key: String, pattern: String, source: MessageSource, sourceName: String) extends Positional

  /**
   * Message file Parser.
   */
  private[i18n] class MessagesParser(messageSource: MessageSource, messageSourceName: String) extends RegexParsers {

    case class Comment(msg: String)

    override def skipWhitespace = false
    override val whiteSpace = """^[ \t]+""".r

    def namedError[A](p: Parser[A], msg: String) = Parser[A] { i =>
      p(i) match {
        case Failure(_, in) => Failure(msg, in)
        case o => o
      }
    }

    val end = """^\s*""".r
    val newLine = namedError((("\r"?) ~> "\n"), "End of line expected")
    val ignoreWhiteSpace = opt(whiteSpace)
    val blankLine = ignoreWhiteSpace <~ newLine ^^ { case _ => Comment("") }

    val comment = """^#.*""".r ^^ { case s => Comment(s) }

    val messageKey = namedError("""^[a-zA-Z0-9_.-]+""".r, "Message key expected")

    val messagePattern = namedError(
      rep(
        ("""\""" ^^ (_ => "")) ~> ( // Ignore the leading \
          ("\r"?) ~> "\n" ^^ (_ => "") | // Ignore escaped end of lines \
          "n" ^^ (_ => "\n") | // Translate literal \n to real newline
          """\""" | // Handle escaped \\
          "^.".r ^^ ("""\""" + _)
        ) |
          "^.".r // Or any character
      ) ^^ { case chars => chars.mkString },
      "Message pattern expected"
    )

    val message = ignoreWhiteSpace ~ messageKey ~ (ignoreWhiteSpace ~ "=" ~ ignoreWhiteSpace) ~ messagePattern ^^ {
      case (_ ~ k ~ _ ~ v) => Messages.Message(k, v.trim, messageSource, messageSourceName)
    }

    val sentence = (comment | positioned(message)) <~ newLine

    val parser = phrase(((sentence | blankLine).*) <~ end) ^^ {
      case messages => messages.collect {
        case m @ Messages.Message(_, _, _, _) => m
      }
    }

    def parse: Either[PlayException.ExceptionSource, Seq[Message]] = {
      parser(new CharSequenceReader(messageSource.read + "\n")) match {
        case Success(messages, _) => Right(messages)
        case NoSuccess(message, in) => Left(
          new PlayException.ExceptionSource("Configuration error", message) {
            def line = in.pos.line
            def position = in.pos.column - 1
            def input = messageSource.read
            def sourceName = messageSourceName
          }
        )
      }
    }

  }

}

/**
 * Provides messages for a particular language.
 *
 * This intended for use to carry both the messages and the current language, particularly useful in templates so that
 * both can be captured by one parameter.
 *
 * @param lang The lang (context)
 * @param messages The messages
 */
case class Messages(lang: Lang, messages: MessagesApi) {

  /**
   * Translates a message.
   *
   * Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param key the message key
   * @param args the message arguments
   * @return the formatted message or a default rendering if the key wasn’t defined
   */
  def apply(key: String, args: Any*): String = messages(key, args: _*)(lang)

  /**
   * Translates the first defined message.
   *
   * Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param keys the message key
   * @param args the message arguments
   * @return the formatted message or a default rendering if the key wasn’t defined
   */
  def apply(keys: Seq[String], args: Any*): String = messages(keys, args: _*)(lang)

  /**
   * Translates a message.
   *
   * Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param key the message key
   * @param args the message arguments
   * @return the formatted message, if this key was defined
   */
  def translate(key: String, args: Seq[Any]): Option[String] = messages.translate(key, args)(lang)

  /**
   * Check if a message key is defined.
   * @param key the message key
   * @return a boolean
   */
  def isDefinedAt(key: String): Boolean = messages.isDefinedAt(key)(lang)
}

/**
 * The internationalisation API.
 */
trait MessagesApi {

  /**
   * Get all the defined messages
   */
  def messages: Map[String, Map[String, String]]

  /**
   * Get the preferred messages for the given candidates.
   *
   * Will select a language from the candidates, based on the languages available, and fallback to the default language
   * if none of the candidates are available.
   */
  def preferred(candidates: Seq[Lang]): Messages

  /**
   * Get the preferred messages for the given request
   */
  def preferred(request: RequestHeader): Messages

  /**
   * Get the preferred messages for the given Java request
   */
  def preferred(request: play.mvc.Http.RequestHeader): Messages

  /**
   * Set the language on the result
   */
  def setLang(result: Result, lang: Lang): Result

  def clearLang(result: Result): Result

  /**
   * Translates a message.
   *
   * Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param key the message key
   * @param args the message arguments
   * @return the formatted message or a default rendering if the key wasn’t defined
   */
  def apply(key: String, args: Any*)(implicit lang: Lang): String

  /**
   * Translates the first defined message.
   *
   * Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param keys the message key
   * @param args the message arguments
   * @return the formatted message or a default rendering if the key wasn’t defined
   */
  def apply(keys: Seq[String], args: Any*)(implicit lang: Lang): String

  /**
   * Translates a message.
   *
   * Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param key the message key
   * @param args the message arguments
   * @return the formatted message, if this key was defined
   */
  def translate(key: String, args: Seq[Any])(implicit lang: Lang): Option[String]

  /**
   * Check if a message key is defined.
   * @param key the message key
   * @return a boolean
   */
  def isDefinedAt(key: String)(implicit lang: Lang): Boolean

  def langCookieName: String

}

/**
 * The internationalisation API.
 */
@Singleton
class DefaultMessagesApi @Inject() (environment: Environment, configuration: Configuration, langs: Langs) extends MessagesApi {

  private val config = PlayConfig(configuration)

  import java.text._

  protected val messagesPrefix =
    config.getOptionalDeprecated[String]("play.i18n.path", "messages.path")
  val messages: Map[String, Map[String, String]] = loadAllMessages

  def preferred(candidates: Seq[Lang]) = Messages(langs.preferred(candidates), this)

  def preferred(request: RequestHeader) = {
    val maybeLangFromCookie = request.cookies.get(langCookieName)
      .flatMap(c => Lang.get(c.value))
    val lang = langs.preferred(maybeLangFromCookie.toSeq ++ request.acceptLanguages)
    Messages(lang, this)
  }

  def preferred(request: Http.RequestHeader) = preferred(request._underlyingHeader())

  def setLang(result: Result, lang: Lang) = result.withCookies(Cookie(langCookieName, lang.code))

  def clearLang(result: Result) = result.discardingCookies(DiscardingCookie(langCookieName))

  def apply(key: String, args: Any*)(implicit lang: Lang): String = {
    translate(key, args).getOrElse(noMatch(key, args))
  }

  def apply(keys: Seq[String], args: Any*)(implicit lang: Lang): String = {
    keys.foldLeft[Option[String]](None) {
      case (None, key) => translate(key, args)
      case (acc, _) => acc
    }.getOrElse(noMatch(keys.last, args))
  }

  private def noMatch(key: String, args: Seq[Any]) = key

  def translate(key: String, args: Seq[Any])(implicit lang: Lang): Option[String] = {
    val langsToTry: List[Lang] =
      List(lang, Lang(lang.language, ""), Lang("default", ""), Lang("default.play", ""))
    val pattern: Option[String] =
      langsToTry.foldLeft[Option[String]](None)((res, lang) =>
        res.orElse(messages.get(lang.code).flatMap(_.get(key))))
    pattern.map(pattern =>
      new MessageFormat(pattern, lang.toLocale).format(args.map(_.asInstanceOf[java.lang.Object]).toArray))
  }

  def isDefinedAt(key: String)(implicit lang: Lang): Boolean = {
    val langsToTry: List[Lang] = List(lang, Lang(lang.language, ""), Lang("default", ""), Lang("default.play", ""))

    langsToTry.foldLeft[Boolean](false)({ (acc, lang) =>
      acc || messages.get(lang.code).map(_.isDefinedAt(key)).getOrElse(false)
    })
  }

  private def joinPaths(first: Option[String], second: String) = first match {
    case Some(parent) => new java.io.File(parent, second).getPath
    case None => second
  }

  protected def loadMessages(file: String): Map[String, String] = {
    import scala.collection.JavaConverters._

    environment.classLoader.getResources(joinPaths(messagesPrefix, file)).asScala.toList
      .filterNot(url => Resources.isDirectory(environment.classLoader, url)).reverse
      .map { messageFile =>
        Messages.parse(Messages.UrlMessageSource(messageFile), messageFile.toString).fold(e => throw e, identity)
      }.foldLeft(Map.empty[String, String]) { _ ++ _ }
  }

  protected def loadAllMessages: Map[String, Map[String, String]] = {
    langs.availables.map(_.code).map { lang =>
      (lang, loadMessages("messages." + lang))
    }.toMap
      .+("default" -> loadMessages("messages"))
      .+("default.play" -> loadMessages("messages.default"))
  }

  lazy val langCookieName =
    config.getDeprecated[String]("play.i18n.langCookieName", "application.lang.cookie")

}

class I18nModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[Langs].to[DefaultLangs],
      bind[MessagesApi].to[DefaultMessagesApi]
    )
  }
}

/**
 * Injection helper for i18n components
 */
trait I18nComponents {

  def environment: Environment
  def configuration: Configuration

  lazy val messagesApi: MessagesApi = new DefaultMessagesApi(environment, configuration, langs)
  lazy val langs: Langs = new DefaultLangs(configuration)

}
