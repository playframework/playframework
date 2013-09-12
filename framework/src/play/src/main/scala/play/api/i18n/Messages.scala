package play.api.i18n

import scala.language.postfixOps

import play.api._
import play.core._

import java.io._

import scala.util.parsing.input._
import scala.util.parsing.combinator._
import scala.util.matching._
import scala.util.control.NonFatal

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
  def toLocale: java.util.Locale = {
    Option(country).filterNot(_.isEmpty).map(c => new java.util.Locale(language, c)).getOrElse(new java.util.Locale(language))
  }

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
  lazy val code = language + Option(country).filterNot(_.isEmpty).map("-" + _).getOrElse("")

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

  private val SimpleLocale = """([a-zA-Z]{2})""".r
  private val CountryLocale = """([a-zA-Z]{2})-([a-zA-Z]{2}|[0-9]{3})""".r

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

  /**
   * Retrieve Lang availables from the application configuration.
   *
   * {{{
   * application.langs="fr,en,de"
   * }}}
   */
  def availables(implicit app: Application): Seq[Lang] = {
    app.configuration.getString("application.langs").map { langs =>
      langs.split(",").map(_.trim).map { lang =>
        try { Lang(lang) } catch {
          case NonFatal(e) => throw app.configuration.reportError("application.langs", "Invalid language code [" + lang + "]", Some(e))
        }
      }.toSeq
    }.getOrElse(Nil)
  }

  /**
   * Guess the preferred lang in the langs set passed as argument.
   * The first Lang that matches an available Lang wins, otherwise returns the first Lang available in this application.
   */
  def preferred(langs: Seq[Lang])(implicit app: Application): Lang = {
    val all = availables
    langs.collectFirst(Function.unlift { lang =>
      all.find(_.satisfies(lang))
    }).getOrElse(all.headOption.getOrElse(Lang.defaultLang))
  }
}

/**
 * High-level internationalisation API (not available yet).
 *
 * For example:
 * {{{
 * val msgString = Messages("items.found", items.size)
 * }}}
 */
object Messages {

  /**
   * Translates a message.
   *
   * Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param key the message key
   * @param args the message arguments
   * @return the formatted message or a default rendering if the key wasn’t defined
   */
  def apply(key: String, args: Any*)(implicit lang: Lang): String = {
    Play.maybeApplication.flatMap { app =>
      app.plugin[MessagesPlugin].map(_.api.translate(key, args)).getOrElse(throw new Exception("this plugin was not registered or disabled"))
    }.getOrElse(noMatch(key, args))
  }

  /**
   * Check if a message key is defined.
   * @param key the message key
   * @return a boolean
   */
  def isDefinedAt(key: String)(implicit lang: Lang): Boolean = {
    Play.maybeApplication.map { app =>
      app.plugin[MessagesPlugin].map(_.api.isDefinedAt(key)).getOrElse(throw new Exception("this plugin was not registered or disabled"))
    }.getOrElse(false)
  }

  /**
   * Retrieves all messages defined in this application.
   */
  def messages(implicit app: Application): Map[String, Map[String, String]] = {
    app.plugin[MessagesPlugin].map(_.api.messages).getOrElse(throw new Exception("this plugin was not registered or disabled"))
  }

  private def noMatch(key: String, args: Seq[Any]) = key

  private[i18n] case class Message(key: String, pattern: String, input: scalax.io.Input, sourceName: String) extends Positional

  /**
   * Message file Parser.
   */
  private[i18n] class MessagesParser(messageInput: scalax.io.Input, messageSourceName: String) extends RegexParsers {

    case class Comment(msg: String)

    override def skipWhitespace = false
    override val whiteSpace = """[ \t]+""".r

    def namedError[A](p: Parser[A], msg: String) = Parser[A] { i =>
      p(i) match {
        case Failure(_, in) => Failure(msg, in)
        case o => o
      }
    }

    def end = """\s*""".r
    def newLine = namedError((("\r"?) ~> "\n"), "End of line expected")
    def blankLine = ignoreWhiteSpace <~ newLine ^^ { case _ => Comment("") }
    def ignoreWhiteSpace = opt(whiteSpace)

    def comment = """#.*""".r ^^ { case s => Comment(s) }

    def messageKey = namedError("""[a-zA-Z0-9_.-]+""".r, "Message key expected")

    def messagePattern = namedError(
      rep(
        """\""" ~> ("\r"?) ~> "\n" ^^ (_ => "") | // Ignore escaped end of lines \
          """\n""" ^^ (_ => "\n") | // Translate literal \n to real newline
          """\\""" ^^ (_ => """\""") | // Handle escaped \\
          """.""".r // Or any character
      ) ^^ { case chars => chars.mkString },
      "Message pattern expected"
    )

    def message = ignoreWhiteSpace ~ messageKey ~ (ignoreWhiteSpace ~ "=" ~ ignoreWhiteSpace) ~ messagePattern ^^ {
      case (_ ~ k ~ _ ~ v) => Messages.Message(k, v.trim, messageInput, messageSourceName)
    }

    def sentence = (comment | positioned(message)) <~ newLine

    def parser = phrase((sentence | blankLine *) <~ end) ^^ {
      case messages => messages.collect {
        case m @ Messages.Message(_, _, _, _) => m
      }
    }

    def parse = {
      parser(new CharSequenceReader(messageInput.string + "\n")) match {
        case Success(messages, _) => messages
        case NoSuccess(message, in) => {
          throw new PlayException.ExceptionSource("Configuration error", message) {
            def line = in.pos.line
            def position = in.pos.column - 1
            def input = messageInput.string
            def sourceName = messageSourceName
          }
        }
      }
    }

  }

}

/**
 * The internationalisation API.
 */
case class MessagesApi(messages: Map[String, Map[String, String]]) {

  import java.text._

  /**
   * Translates a message.
   *
   * Uses `java.text.MessageFormat` internally to format the message.
   *
   * @param key the message key
   * @param args the message arguments
   * @return the formatted message, if this key was defined
   */
  def translate(key: String, args: Seq[Any])(implicit lang: Lang): Option[String] = {
    val langsToTry: List[Lang] =
      List(lang, Lang(lang.language, ""), Lang("default", ""))
    val pattern: Option[String] =
      langsToTry.foldLeft[Option[String]](None)((res, lang) =>
        res.orElse(messages.get(lang.code).flatMap(_.get(key))))
    pattern.map(pattern =>
      new MessageFormat(pattern, lang.toLocale).format(args.map(_.asInstanceOf[java.lang.Object]).toArray))
  }

  /**
   * Check if a message key is defined.
   * @param key the message key
   * @return a boolean
   */
  def isDefinedAt(key: String)(implicit lang: Lang): Boolean = {
    val langsToTry: List[Lang] = List(lang, Lang(lang.language, ""), Lang("default", ""))

    langsToTry.foldLeft[Boolean](false)({ (acc, lang) =>
      acc || messages.get(lang.code).map(_.isDefinedAt(key)).getOrElse(false)
    })
  }

}

/**
 * Play Plugin for internationalisation.
 */
class MessagesPlugin(app: Application) extends Plugin {

  import scala.collection.JavaConverters._

  import scalax.file._
  import scalax.io.JavaConverters._

  private def loadMessages(file: String): Map[String, String] = {
    app.classloader.getResources(file).asScala.toList.reverse.map { messageFile =>
      new Messages.MessagesParser(messageFile.asInput, messageFile.toString).parse.map { message =>
        message.key -> message.pattern
      }.toMap
    }.foldLeft(Map.empty[String, String]) { _ ++ _ }
  }

  private lazy val messages = {
    MessagesApi {
      Lang.availables(app).map(_.code).map { lang =>
        (lang, loadMessages("messages." + lang))
      }.toMap + ("default" -> loadMessages("messages"))
    }
  }

  /**
   * The underlying internationalisation API.
   */
  def api = messages

  /**
   * Loads all configuration and message files defined in the classpath.
   */
  override def onStart() {
    messages
  }

}
