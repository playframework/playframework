package play.api.i18n

import play.api._
import play.core._

import java.io._

import scala.util.parsing.input._
import scala.util.parsing.combinator._
import scala.util.matching._

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
   * Create a Lang value from a code (such as fr or en-US).
   */
  def apply(code: String): Lang = {
    code match {
      case SimpleLocale(language) => Lang(language, "")
      case CountryLocale(language, country) => Lang(language, country)
      case _ => sys.error("Unrecognized language: %s".format(code))
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
          case e => throw app.configuration.reportError("application.langs", "Invalid language code [" + lang + "]", Some(e))
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
    langs.find(all.contains(_)).getOrElse(all.headOption.getOrElse(Lang.defaultLang))
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

    override def phrase[T](p: Parser[T]) = new Parser[T] {
      lastNoSuccess = null
      def apply(in: Input) = p(in) match {
        case s @ Success(out, in1) =>
          if (in1.atEnd)
            s
          else if (lastNoSuccess == null || lastNoSuccess.next.pos < in1.pos)
            Failure("end of input expected", in1)
          else
            lastNoSuccess
        case _ => lastNoSuccess
      }
    }

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

    def messageKey = namedError("""[a-zA-Z0-9_.]+""".r, "Message key expected")
    def messagePattern = namedError(""".+""".r, "Message pattern expected")
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
      parser(new CharSequenceReader(messageInput.slurpString + "\n")) match {
        case Success(messages, _) => messages
        case NoSuccess(message, in) => {
          throw new PlayException("Configuration error", message) with PlayException.ExceptionSource {
            def line = Some(in.pos.line)
            def position = Some(in.pos.column - 1)
            def input = Some(messageInput)
            def sourceName = Some(messageSourceName)
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
    messages.get(lang.code).flatMap(_.get(key)).orElse(messages.get("default").flatMap(_.get(key))).map { pattern =>
      new MessageFormat(pattern, lang.toLocale).format(args.map(_.asInstanceOf[java.lang.Object]).toArray)
    }
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
