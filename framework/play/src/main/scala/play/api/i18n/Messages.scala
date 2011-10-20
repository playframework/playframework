package play.api.i18n

import play.api._
import play.core._

import java.io._

import scala.util.parsing.input._
import scala.util.parsing.combinator._
import scala.util.matching._

/**
 * High level API for i18n.
 *
 * Example:
 * {{{
 * val msgString = Messages("items.found", items.size)
 * }}}
 */
object Messages {

  /**
   * Translate a message.
   *
   * Use the Java MessageFormat to internally format the message.
   *
   * @param key The message key.
   * @param args The message arguments
   * @return The formatted message or a default rendering if the key wasn't defined.
   */
  def apply(key: String, args: Any*) = {
    Play.maybeApplication.flatMap { app =>
      app.plugin[MessagesPlugin].api.translate(key, args)
    }.getOrElse(noMatch(key, args))
  }

  private def noMatch(key: String, args: Seq[Any]) = {
    key + Option(args.map(_.toString).mkString(",")).filterNot(_.isEmpty).map("(" + _ + ")").getOrElse("")
  }

  /**
   * An i18n message.
   *
   * @param key The message key
   * @param pattern The message pattern
   * @param input The source from which this message was read.
   * @param sourceName The source name from which this message was read.
   */
  case class Message(key: String, pattern: String, input: scalax.io.Input, sourceName: String) extends Positional

  /**
   * Message files parser.
   */
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
    def newLine = namedError("\n", "End of line expected")
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
      parser(new CharSequenceReader(messageInput.slurpString)) match {
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
 * The i18n APO
 */
case class MessagesApi(messages: Map[String, String]) {

  import java.text._

  /**
   * Translate a message.
   *
   * Use the Java MessageFormat to internally format the message.
   *
   * @param key The message key.
   * @param args The message arguments
   * @return Maybe the formatted message if this key was defined.
   */
  def translate(key: String, args: Seq[Any]): Option[String] = {
    messages.get(key).map { pattern =>
      MessageFormat.format(pattern, args.map(_.asInstanceOf[java.lang.Object]): _*)
    }
  }

}

/**
 * Play Plugin for i18n
 */
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

  /**
   * The underlying i18n API
   */
  def api = messages

  /**
   * Load all conf/messages files defined in the classpath.
   */
  override def onStart {
    messages
  }

}