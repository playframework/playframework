package play.api.i18n

import play.api._
import play.core._

import java.io._

import scala.util.parsing.input._
import scala.util.parsing.combinator._
import scala.util.matching._

/**
 * High-level internationalisation API.
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
  def apply(key: String, args: Any*) = {
    Play.maybeApplication.flatMap { app =>
      app.plugin[MessagesPlugin].map(_.api.translate(key, args)).getOrElse(throw new Exception("this plugin was not registered or disabled"))
    }.getOrElse(noMatch(key, args))
  }

  private def noMatch(key: String, args: Seq[Any]) = {
    key + Option(args.map(_.toString).mkString(",")).filterNot(_.isEmpty).map("(" + _ + ")").getOrElse("")
  }

  /**
   * An internationalised message.
   *
   * @param key the message key
   * @param pattern the message pattern
   * @param input the source from which this message was read
   * @param sourceName the source name from which this message was read
   */
  case class Message(key: String, pattern: String, input: scalax.io.Input, sourceName: String) extends Positional

  /** Message file parser. */
  class MessagesParser(messageInput: scalax.io.Input, messageSourceName: String) extends RegexParsers {

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

/** The internationalisation API. */
case class MessagesApi(messages: Map[String, String]) {

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
  def translate(key: String, args: Seq[Any]): Option[String] = {
    messages.get(key).map { pattern =>
      MessageFormat.format(pattern, args.map(_.asInstanceOf[java.lang.Object]): _*)
    }
  }

}

/** Play Plugin for internationalisation. */
class MessagesPlugin(app: Application) extends Plugin {

  import scala.collection.JavaConverters._

  import scalax.file._
  import scalax.io.JavaConverters._

  private lazy val messages = {
    MessagesApi {
      app.classloader.getResources("conf/messages").asScala.map { messageFile =>
        new Messages.MessagesParser(messageFile.asInput, messageFile.toString).parse.map { message =>
          message.key -> message.pattern
        }.toMap
      }.foldLeft(Map.empty[String, String]) { _ ++ _ }
    }
  }

  /** The underlying internationalisation API. */
  def api = messages

  /** Loads all configuration and message files defined in the classpath. */
  override def onStart {
    messages
  }

}
