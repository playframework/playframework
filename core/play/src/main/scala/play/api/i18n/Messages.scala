/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.i18n

import java.net.URL

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import play.api._
import play.api.http.HttpConfiguration
import play.api.libs.typedmap.TypedKey
import play.api.mvc.Cookie.SameSite
import play.api.mvc._
import play.libs.Scala
import play.mvc.Http
import play.utils.PlayIO
import play.utils.Resources

import scala.annotation.implicitNotFound
import scala.concurrent.duration.FiniteDuration
import scala.io.Codec
import scala.language._
import scala.util.parsing.combinator._
import scala.util.parsing.input._

/**
 * Internationalisation API.
 *
 * For example:
 * {{{
 * val msgString = Messages("items.found", items.size)
 * }}}
 */
object Messages extends MessagesImplicits {

  /**
   * Request Attributes for the MessagesApi
   * Currently all Attributes are only available inside the [[MessagesApi]] methods.
   */
  object Attrs {
    val CurrentLang: TypedKey[Lang] = TypedKey("CurrentLang")
  }

  private[play] val messagesApiCache = Application.instanceCache[MessagesApi]

  /**
   * Translates a message.
   *
   * Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param key  the message key
   * @param args the message arguments
   * @return the formatted message or a default rendering if the key wasn’t defined
   */
  def apply(key: String, args: Any*)(implicit provider: MessagesProvider): String = {
    provider.messages(key, args: _*)
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
  def apply(keys: Seq[String], args: Any*)(implicit provider: MessagesProvider): String = {
    provider.messages(keys, args: _*)
  }

  /**
   * Check if a message key is defined.
   *
   * @param key the message key
   * @return a boolean
   */
  def isDefinedAt(key: String)(implicit provider: MessagesProvider): Boolean = {
    provider.messages.isDefinedAt(key)
  }

  /**
   * Parse all messages of a given input.
   */
  def parse(
      messageSource: MessageSource,
      messageSourceName: String
  ): Either[PlayException.ExceptionSource, Map[String, String]] = {
    new Messages.MessagesParser(messageSource, "").parse.right.map { messages =>
      messages.iterator.map(message => message.key -> message.pattern).toMap
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

  private[i18n] case class Message(key: String, pattern: String, source: MessageSource, sourceName: String)
      extends Positional

  /**
   * Message file Parser.
   */
  private[i18n] class MessagesParser(messageSource: MessageSource, messageSourceName: String) extends RegexParsers {
    override val whiteSpace = """^[ \t]+""".r
    val end                 = """^\s*""".r
    val newLine             = namedError((("\r" ?) ~> "\n"), "End of line expected")
    val ignoreWhiteSpace    = opt(whiteSpace)
    val blankLine           = ignoreWhiteSpace <~ newLine ^^ (_ => Comment(""))
    val comment             = """^#.*""".r ^^ (s => Comment(s))
    val messageKey =
      namedError("""^[a-zA-Z0-9$_.-]+""".r, "Message key expected")

    val messagePattern = namedError(
      rep(
        ("""\""" ^^ (_ => "")) ~> (// Ignore the leading \
        ("\r" ?) ~> "\n" ^^ (_ => "") | // Ignore escaped end of lines \
          "n" ^^ (_ => "\n") |          // Translate literal \n to real newline
          """\""" |                     // Handle escaped \\
          "^.".r ^^ ("""\""" + _)) |
          "^.".r // Or any character
      ) ^^ (_.mkString),
      "Message pattern expected"
    )
    val message = ignoreWhiteSpace ~ messageKey ~ (ignoreWhiteSpace ~ "=" ~ ignoreWhiteSpace) ~ messagePattern ^^ {
      case (_ ~ k ~ _ ~ v) =>
        Messages.Message(k, v.trim, messageSource, messageSourceName)
    }
    val sentence = (comment | positioned(message)) <~ newLine
    val parser = phrase(((sentence | blankLine).*) <~ end) ^^ { messages =>
      messages.collect { case m: Messages.Message => m }
    }

    override def skipWhitespace = false

    def namedError[A](p: Parser[A], msg: String) = Parser[A] { i =>
      p(i) match {
        case Failure(_, in) => Failure(msg, in)
        case o              => o
      }
    }

    def parse: Either[PlayException.ExceptionSource, Seq[Message]] = {
      parser(new CharSequenceReader(messageSource.read + "\n")) match {
        case Success(messages, _) => Right(messages)
        case NoSuccess(message, in) =>
          Left(
            new PlayException.ExceptionSource("Configuration error", message) {
              def line       = in.pos.line
              def position   = in.pos.column - 1
              def input      = messageSource.read
              def sourceName = messageSourceName
            }
          )
      }
    }

    case class Comment(msg: String)
  }
}

/**
 * Provides messages for a particular language.
 *
 * This intended for use to carry both the messages and the current language,
 * particularly useful in templates so that both can be captured by one
 * parameter.
 *
 * @param lang        The lang (context)
 * @param messagesApi The messages API
 */
case class MessagesImpl(lang: Lang, messagesApi: MessagesApi) extends Messages {

  /**
   * Translates a message.
   *
   * Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param key  the message key
   * @param args the message arguments
   * @return the formatted message or a default rendering if the key wasn’t defined
   */
  override def apply(key: String, args: Any*): String = {
    messagesApi(key, args: _*)(lang)
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
  override def apply(keys: Seq[String], args: Any*): String = {
    messagesApi(keys, args: _*)(lang)
  }

  /**
   * Translates a message.
   *
   * Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param key  the message key
   * @param args the message arguments
   * @return the formatted message, if this key was defined
   */
  override def translate(key: String, args: Seq[Any]): Option[String] = {
    messagesApi.translate(key, args)(lang)
  }

  /**
   * Check if a message key is defined.
   *
   * @param key the message key
   * @return a boolean
   */
  override def isDefinedAt(key: String): Boolean = {
    messagesApi.isDefinedAt(key)(lang)
  }

  /**
   * @return the Java version for this Messages.
   */
  override def asJava: play.i18n.Messages =
    new play.i18n.MessagesImpl(lang.asJava, messagesApi.asJava)
}

/**
 * A messages returns string messages using a chosen language.
 *
 * This is commonly backed by a MessagesImpl case class, but does
 * extend Product and does not expose MessagesApi as part of
 * its interface.
 */
@implicitNotFound(
  "An implicit Messages instance was not found.  Please see https://www.playframework.com/documentation/latest/ScalaI18N"
)
trait Messages extends MessagesProvider {

  /**
   * Every Messages is also a MessagesProvider.
   *
   * @return the messages itself.
   */
  def messages: Messages = this

  /**
   * Returns the language associated with the messages.
   *
   * @return the selected language.
   */
  def lang: Lang

  /**
   * Translates a message.
   *
   * Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param key  the message key
   * @param args the message arguments
   * @return the formatted message or a default rendering if the key wasn’t defined
   */
  def apply(key: String, args: Any*): String

  /**
   * Translates the first defined message.
   *
   * Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param keys the message key
   * @param args the message arguments
   * @return the formatted message or a default rendering if the key wasn’t defined
   */
  def apply(keys: Seq[String], args: Any*): String

  /**
   * Translates a message.
   *
   * Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param key  the message key
   * @param args the message arguments
   * @return the formatted message, if this key was defined
   */
  def translate(key: String, args: Seq[Any]): Option[String]

  /**
   * Check if a message key is defined.
   *
   * @param key the message key
   * @return a boolean
   */
  def isDefinedAt(key: String): Boolean

  /**
   * @return the Java version for this Messages.
   */
  def asJava: play.i18n.Messages
}

/**
 * This trait is used to indicate when a Messages instance can be produced.
 */
@implicitNotFound(
  "An implicit MessagesProvider instance was not found.  Please see https://www.playframework.com/documentation/latest/ScalaForms#Passing-MessagesProvider-to-Form-Helpers"
)
trait MessagesProvider {
  def messages: Messages
}

trait MessagesImplicits {
  implicit def implicitMessagesProviderToMessages(implicit messagesProvider: MessagesProvider): Messages = {
    messagesProvider.messages
  }
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
   * Translates a message.
   *
   * Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param key  the message key
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
   * @param key  the message key
   * @param args the message arguments
   * @return the formatted message, if this key was defined
   */
  def translate(key: String, args: Seq[Any])(implicit lang: Lang): Option[String]

  /**
   * Check if a message key is defined.
   *
   * @param key the message key
   * @return a boolean
   */
  def isDefinedAt(key: String)(implicit lang: Lang): Boolean

  /**
   * Given a [[Result]] and a [[Lang]], return a new [[Result]] with the lang cookie set to the given [[Lang]].
   */
  def setLang(result: Result, lang: Lang): Result

  /**
   * Given a [[Result]], return a new [[Result]] with the lang cookie discarded.
   */
  def clearLang(result: Result): Result

  /**
   * Name for the language Cookie.
   */
  def langCookieName: String

  /**
   * An optional max age in seconds for the language Cookie.
   */
  def langCookieMaxAge: Option[Int]

  /**
   * Whether the secure attribute of the cookie is true or not.
   */
  def langCookieSecure: Boolean

  /**
   * Whether the HTTP only attribute of the cookie should be set to true or not.
   */
  def langCookieHttpOnly: Boolean

  /**
   * The value of the [[SameSite]] attribute of the cookie. If None, then no SameSite
   * attribute is set.
   */
  def langCookieSameSite: Option[SameSite]

  /**
   * @return The Java version for Messages API.
   */
  def asJava: play.i18n.MessagesApi = new play.i18n.MessagesApi(this)
}

/**
 * The Messages API.
 */
@Singleton
class DefaultMessagesApi @Inject() (
    val messages: Map[String, Map[String, String]] = Map.empty,
    langs: Langs = new DefaultLangs(),
    val langCookieName: String = "PLAY_LANG",
    val langCookieSecure: Boolean = false,
    val langCookieHttpOnly: Boolean = false,
    val langCookieSameSite: Option[SameSite] = None,
    val httpConfiguration: HttpConfiguration = HttpConfiguration(),
    val langCookieMaxAge: Option[Int] = None
) extends MessagesApi {
  // Java API
  def this(javaMessages: java.util.Map[String, java.util.Map[String, String]], langs: play.i18n.Langs) = {
    this(
      Scala.asScala(javaMessages).map { case (k, v) => (k, Scala.asScala(v)) },
      langs.asScala(),
      "PLAY_LANG",
      false,
      false,
      None,
      HttpConfiguration(),
      None
    )
  }

  // Java API
  def this(messages: java.util.Map[String, java.util.Map[String, String]]) =
    this(messages, new DefaultLangs().asJava)

  import java.text._

  override def preferred(candidates: Seq[Lang]): Messages =
    MessagesImpl(langs.preferred(candidates), this)

  override def preferred(request: Http.RequestHeader): Messages =
    preferred(request.asScala())

  override def preferred(request: RequestHeader): Messages = {
    val maybeLangFromRequest = request.transientLang()
    val maybeLangFromCookie =
      request.cookies.get(langCookieName).flatMap(c => Lang.get(c.value))
    val lang = langs.preferred(maybeLangFromRequest.toSeq ++ maybeLangFromCookie.toSeq ++ request.acceptLanguages)
    MessagesImpl(lang, this)
  }

  override def apply(key: String, args: Any*)(implicit lang: Lang): String = {
    translate(key, args).getOrElse(noMatch(key, args))
  }

  override def apply(keys: Seq[String], args: Any*)(implicit lang: Lang): String = {
    keys
      .foldLeft(Option.empty[String])((acc, key) => acc.orElse(translate(key, args)))
      .getOrElse(noMatch(keys.last, args))
  }

  protected def noMatch(key: String, args: Seq[Any])(implicit lang: Lang): String = key

  override def translate(key: String, args: Seq[Any])(implicit lang: Lang): Option[String] = {
    val codesToTry = Seq(lang.code, lang.language, "default", "default.play")
    val pattern = codesToTry.foldLeft(Option.empty[String]) { (res, lang) =>
      res.orElse(for {
        messages <- messages.get(lang)
        message  <- messages.get(key)
      } yield message)
    }
    pattern.map(p =>
      new MessageFormat(p, lang.toLocale)
        .format(args.map(_.asInstanceOf[Object]).toArray)
    )
  }

  override def isDefinedAt(key: String)(implicit lang: Lang): Boolean = {
    val codesToTry = Seq(lang.code, lang.language, "default", "default.play")
    codesToTry.foldLeft(false)((acc, lang) => acc || messages.get(lang).exists(_.isDefinedAt(key)))
  }

  override def setLang(result: Result, lang: Lang): Result = {
    val cookie = Cookie(
      langCookieName,
      lang.code,
      maxAge = langCookieMaxAge,
      path = httpConfiguration.session.path,
      domain = httpConfiguration.session.domain,
      secure = langCookieSecure,
      httpOnly = langCookieHttpOnly,
      sameSite = langCookieSameSite
    )
    result.withCookies(cookie)
  }

  override def clearLang(result: Result): Result = {
    val discardingCookie = DiscardingCookie(
      langCookieName,
      path = httpConfiguration.session.path,
      domain = httpConfiguration.session.domain,
      secure = langCookieSecure
    )
    result.discardingCookies(discardingCookie)
  }
}

@Singleton
class DefaultMessagesApiProvider @Inject() (
    environment: Environment,
    config: Configuration,
    langs: Langs,
    httpConfiguration: HttpConfiguration
) extends Provider[MessagesApi] {
  override lazy val get: MessagesApi = {
    new DefaultMessagesApi(
      loadAllMessages,
      langs,
      langCookieName = langCookieName,
      langCookieSecure = langCookieSecure,
      langCookieHttpOnly = langCookieHttpOnly,
      langCookieSameSite = langCookieSameSite,
      httpConfiguration = httpConfiguration,
      langCookieMaxAge = langCookieMaxAge
    )
  }

  def langCookieName =
    config.getDeprecated[String]("play.i18n.langCookieName", "application.lang.cookie")
  def langCookieSecure   = config.get[Boolean]("play.i18n.langCookieSecure")
  def langCookieHttpOnly = config.get[Boolean]("play.i18n.langCookieHttpOnly")
  def langCookieSameSite =
    HttpConfiguration.parseSameSite(config, "play.i18n.langCookieSameSite")
  def langCookieMaxAge =
    config
      .get[Option[FiniteDuration]]("play.i18n.langCookieMaxAge")
      .map(_.toSeconds.toInt)

  protected def loadAllMessages: Map[String, Map[String, String]] = {
    langs.availables.iterator
      .map(lang => lang.code -> loadMessages(s"messages.${lang.code}"))
      .toMap[String, Map[String, String]]
      .updated("default", loadMessages("messages"))
      .updated("default.play", loadMessages("messages.default"))
  }

  protected def loadMessages(file: String): Map[String, String] = {
    import scala.collection.JavaConverters._

    environment.classLoader
      .getResources(joinPaths(messagesPrefix, file))
      .asScala
      .toList
      .filterNot(url => Resources.isDirectory(environment.classLoader, url))
      .reverse
      .map { messageFile =>
        Messages
          .parse(Messages.UrlMessageSource(messageFile), messageFile.toString)
          .fold(e => throw e, identity)
      }
      .foldLeft(Map.empty[String, String])(_ ++ _)
  }

  protected def messagesPrefix =
    config.getDeprecated[Option[String]]("play.i18n.path", "messages.path")

  protected def joinPaths(first: Option[String], second: String) = first match {
    case Some(parent) => new java.io.File(parent, second).getPath
    case None         => second
  }
}
