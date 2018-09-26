/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.i18n

import java.net.URL
import java.util.Collections
import java.util.function.Function
import java.util.stream.Collectors
import javax.inject.{ Inject, Provider, Singleton }

import play.api._
import play.api.http.HttpConfiguration
import play.api.libs.typedmap.TypedKey
import play.api.mvc._
import play.libs.Scala
import play.mvc.Http
import play.utils.{ PlayIO, Resources }

import scala.annotation.implicitNotFound
import scala.collection.mutable
import scala.collection.breakOut
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
   * Implicit conversions providing [[Messages]] or [[MessagesApi]].
   *
   * The implicit [[Application]] is deprecated as Application should only be
   * exposed to the underlying module system.
   */
  object Implicits {

    import scala.language.implicitConversions

    /**
     * @deprecated Since 2.6.0, please use an injected [[MessagesApi]].
     */
    @deprecated("See https://www.playframework.com/documentation/2.6.x/MessagesMigration26", "2.6.0")
    implicit def applicationMessagesApi(implicit application: Application): MessagesApi =
      messagesApiCache(application)

    /**
     * @deprecated Since 2.6.0, please use messagesApi.preferred(Seq(lang)).
     */
    @deprecated("See https://www.playframework.com/documentation/2.6.x/MessagesMigration26", "2.6.0")
    implicit def applicationMessages(implicit lang: Lang, application: Application): Messages =
      MessagesImpl(lang, messagesApiCache(application))
  }

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
  def parse(messageSource: MessageSource, messageSourceName: String): Either[PlayException.ExceptionSource, Map[String, String]] = {
    new Messages.MessagesParser(messageSource, "").parse.right.map { messages =>
      messages.map { message => message.key -> message.pattern }(breakOut)
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

    override val whiteSpace = """^[ \t]+""".r
    val end = """^\s*""".r
    val newLine = namedError((("\r" ?) ~> "\n"), "End of line expected")
    val ignoreWhiteSpace = opt(whiteSpace)
    val blankLine = ignoreWhiteSpace <~ newLine ^^ { case _ => Comment("") }
    val comment = """^#.*""".r ^^ { case s => Comment(s) }
    val messageKey = namedError("""^[a-zA-Z0-9$_.-]+""".r, "Message key expected")
    val messagePattern = namedError(
      rep(
        ("""\""" ^^ (_ => "")) ~> ( // Ignore the leading \
          ("\r" ?) ~> "\n" ^^ (_ => "") | // Ignore escaped end of lines \
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

    override def skipWhitespace = false

    def namedError[A](p: Parser[A], msg: String) = Parser[A] { i =>
      p(i) match {
        case Failure(_, in) => Failure(msg, in)
        case o => o
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
}

/**
 * A messages returns string messages using a chosen language.
 *
 * This is commonly backed by a MessagesImpl case class, but does
 * extend Product and does not expose MessagesApi as part of
 * its interface.
 */
@implicitNotFound("An implicit Messages instance was not found.  Please see https://www.playframework.com/documentation/latest/ScalaI18N")
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
}

/**
 * This trait is used to indicate when a Messages instance can be produced.
 */
@implicitNotFound("An implicit MessagesProvider instance was not found.  Please see https://www.playframework.com/documentation/2.6.x/ScalaForms#Passing-MessagesProvider-to-Form-Helpers")
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
   * Set the language on the result
   */
  def setLang(result: Result, lang: Lang): Result

  def clearLang(result: Result): Result

  def langCookieName: String

  def langCookieSecure: Boolean

  def langCookieHttpOnly: Boolean

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
    val httpConfiguration: HttpConfiguration = HttpConfiguration()) extends MessagesApi {

  // Java API
  def this(javaMessages: java.util.Map[String, java.util.Map[String, String]], langs: play.i18n.Langs) = {
    this(
      Scala.asScala(javaMessages).map { case (k, v) => (k, Scala.asScala(v)) },
      langs.asScala(),
      "PLAY_LANG",
      false,
      false,
      HttpConfiguration()
    )
  }

  // Java API
  def this(messages: java.util.Map[String, java.util.Map[String, String]]) = {
    this(messages, new DefaultLangs().asJava)
  }

  import java.text._

  override def preferred(candidates: Seq[Lang]): Messages = {
    MessagesImpl(langs.preferred(candidates), this)
  }

  override def preferred(request: Http.RequestHeader): Messages = {
    preferred(request.asScala())
  }

  override def preferred(request: RequestHeader): Messages = {
    val maybeLangFromContext = request.attrs.get(Messages.Attrs.CurrentLang)
    val maybeLangFromCookie = request.cookies.get(langCookieName).flatMap(c => Lang.get(c.value))
    val lang = langs.preferred(maybeLangFromContext.toSeq ++ maybeLangFromCookie.toSeq ++ request.acceptLanguages)
    MessagesImpl(lang, this)
  }

  override def apply(key: String, args: Any*)(implicit lang: Lang): String = {
    translate(key, args).getOrElse(noMatch(key, args))
  }

  override def apply(keys: Seq[String], args: Any*)(implicit lang: Lang): String = {
    keys.foldLeft[Option[String]](None) {
      case (None, key) => translate(key, args)
      case (acc, _) => acc
    }.getOrElse(noMatch(keys.last, args))
  }

  protected def noMatch(key: String, args: Seq[Any])(implicit lang: Lang): String = key

  override def translate(key: String, args: Seq[Any])(implicit lang: Lang): Option[String] = {
    val codesToTry = Seq(lang.code, lang.language, "default", "default.play")
    val pattern: Option[String] =
      codesToTry.foldLeft[Option[String]](None)((res, lang) =>
        res.orElse(messages.get(lang).flatMap(_.get(key))))
    pattern.map(pattern =>
      new MessageFormat(pattern, lang.toLocale).format(args.map(_.asInstanceOf[java.lang.Object]).toArray))
  }

  override def isDefinedAt(key: String)(implicit lang: Lang): Boolean = {
    val codesToTry = Seq(lang.code, lang.language, "default", "default.play")

    codesToTry.foldLeft[Boolean](false)({ (acc, lang) =>
      acc || messages.get(lang).exists(_.isDefinedAt(key))
    })
  }

  override def setLang(result: Result, lang: Lang): Result = {
    result.withCookies(Cookie(langCookieName, lang.code,
      path = httpConfiguration.session.path,
      domain = httpConfiguration.session.domain,
      secure = langCookieSecure,
      httpOnly = langCookieHttpOnly))
  }

  override def clearLang(result: Result): Result = {
    result.discardingCookies(DiscardingCookie(
      langCookieName,
      path = httpConfiguration.session.path,
      domain = httpConfiguration.session.domain,
      secure = langCookieSecure))
  }

}

@Singleton
class DefaultMessagesApiProvider @Inject() (
    environment: Environment,
    config: Configuration,
    langs: Langs,
    httpConfiguration: HttpConfiguration)
  extends Provider[MessagesApi] {

  override lazy val get: MessagesApi = {
    new DefaultMessagesApi(
      loadAllMessages,
      langs,
      langCookieName = langCookieName,
      langCookieSecure = langCookieSecure,
      langCookieHttpOnly = langCookieHttpOnly,
      httpConfiguration = httpConfiguration
    )
  }

  def langCookieName =
    config.getDeprecated[String]("play.i18n.langCookieName", "application.lang.cookie")

  def langCookieSecure =
    config.get[Boolean]("play.i18n.langCookieSecure")

  def langCookieHttpOnly =
    config.get[Boolean]("play.i18n.langCookieHttpOnly")

  protected def loadAllMessages: Map[String, Map[String, String]] = {
    (langs.availables.map { lang =>
      val code = lang.code
      code -> loadMessages(s"messages.${code}")
    }(breakOut): Map[String, Map[String, String]]).
      +("default" -> loadMessages("messages")) + (
        "default.play" -> loadMessages("messages.default"))
  }

  protected def loadMessages(file: String): Map[String, String] = {
    import scala.collection.JavaConverters._

    environment.classLoader.getResources(joinPaths(messagesPrefix, file)).asScala.toList
      .filterNot(url => Resources.isDirectory(environment.classLoader, url)).reverse
      .map { messageFile =>
        Messages.parse(Messages.UrlMessageSource(messageFile), messageFile.toString).fold(e => throw e, identity)
      }.foldLeft(Map.empty[String, String]) {
        _ ++ _
      }
  }

  protected def messagesPrefix = config.getDeprecated[Option[String]]("play.i18n.path", "messages.path")

  protected def joinPaths(first: Option[String], second: String) = first match {
    case Some(parent) => new java.io.File(parent, second).getPath
    case None => second
  }

}
