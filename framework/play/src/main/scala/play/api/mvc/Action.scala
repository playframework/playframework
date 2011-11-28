package play.api.mvc

import play.api.libs.iteratee._

trait Handler

/**
 * An action is essentially a (Request[A] => Result) function that
 * handles a request and generates a result to be sent to the client.
 *
 * For example,
 * {{{
 * val echo = Action { request =>
 *   Ok("Got request [" + request + "]")
 * }
 * }}}
 *
 * @tparam A the type of the request body
 */
trait Action[A] extends (Request[A] => Result) with Handler {

  /** Type of the request body. */
  type BODY_CONTENT = A

  /**
   * Body parser associated with this action.
   *
   * @see BodyParser
   */
  def parser: BodyParser[A]

  /**
   * Invokes this action.
   *
   * @param request the incoming HTTP request
   * @return the result to be sent to the client
   */
  def apply(request: Request[A]): Result

  /**
   * Composes this action with another action.
   *
   * For example:
   * {{{
   *   val actionWithLogger = anyAction.compose { (request, originalAction) =>
   *     Logger.info("Invoking " + originalAction)
   *     val result = originalAction(request)
   *     Logger.info("Got result: " + result)
   *     result
   *   }
   * }}}
   */
  def compose(composer: (Request[A], Action[A]) => Result) = {
    val self = this
    new Action[A] {
      def parser = self.parser
      def apply(request: Request[A]) = composer(request, self)
    }
  }

  /**
   * Returns itself, for better support in the routes file.
   *
   * @return itself
   */
  def apply() = this

}

/**
 * A body parser parses the HTTP request body content.
 *
 * @tparam T the body content type
 */
trait BodyParser[+T] extends Function1[RequestHeader, Iteratee[Array[Byte], T]]

/** Helper object to construct `BodyParser` values. */
object BodyParser {

  def apply[T](f: Function1[RequestHeader, Iteratee[Array[Byte], T]]) = new BodyParser[T] {
    def apply(rh: RequestHeader) = f(rh)
  }

}

sealed trait AnyContent {

  def asUrlFormEncoded: Map[String, Seq[String]] = this match {
    case AnyContentAsUrlFormEncoded(data) => data
    case _ => sys.error("Oops")
  }

  def asText: String = this match {
    case AnyContentAsText(txt) => txt
    case _ => sys.error("Oops")
  }

  def asRaw: Array[Byte] = this match {
    case AnyContentUnknown(raw) => raw
    case _ => sys.error("Oops")
  }

}

case class AnyContentAsText(txt: String) extends AnyContent
case class AnyContentAsUrlFormEncoded(data: Map[String, Seq[String]]) extends AnyContent
case class AnyContentUnknown(raw: Array[Byte]) extends AnyContent

object AnyContent {

  import scala.collection.mutable._
  import scala.collection.JavaConverters._

  import play.core.UrlEncodedParser
  import play.api.http.HeaderNames._

  def parser: BodyParser[AnyContent] = BodyParser { requestHeader =>
    val body = Iteratee.fold[Array[Byte], ArrayBuffer[Byte]](ArrayBuffer[Byte]())(_ ++= _)
    body.mapDone { content =>
      val contentTypeHeader = requestHeader.headers.get(CONTENT_TYPE).map(_.split(";"))
      val (contentType, charset) = (contentTypeHeader.flatMap(_.headOption.map(_.trim)).getOrElse(""),
        contentTypeHeader.flatMap(_.tail.headOption.map(_.trim).filter(_.startsWith("charset=")).flatMap(_.split('=').tail.headOption)).getOrElse("utf-8"))
      contentType match {
        case "text/plain" => AnyContentAsText(new String(content.toArray, charset))
        case "application/x-www-form-urlencoded" => AnyContentAsUrlFormEncoded(
          UrlEncodedParser.parse(new String(content.toArray, charset), charset).asScala.toMap.mapValues(_.toSeq))
        case _ => AnyContentUnknown(content.toArray)
      }
    }
  }

}

/** Helper object to create `Action` values. */
object Action {

  /**
   * Constructs an `Action`.
   *
   * For example:
   * {{{
   * val echo = Action(anyContentParser) { request =>
   *   Ok("Got request [" + request + "]")
   * }
   * }}}
   *
   * @tparam A the type of the request body
   * @param bodyParser the `BodyParser` to use to parse the request body
   * @param block the action code
   * @return an action
   */
  def apply[A](bodyParser: BodyParser[A], block: Request[A] => Result): Action[A] = new Action[A] {
    def parser = bodyParser
    def apply(ctx: Request[A]) = block(ctx)
  }

  /**
   * Constructs an `Action` with default content.
   *
   * For example:
   * {{{
   * val echo = Action { request =>
   *   Ok("Got request [" + request + "]")
   * }
   * }}}
   *
   * @param block the action code
   * @return an action
   */
  def apply(block: Request[AnyContent] => Result): Action[AnyContent] = {
    Action(AnyContent.parser, block)
  }

  /**
   * Constructs an `Action` with default content, and no request parameter.
   *
   * For example:
   * {{{
   * val hello = Action {
   *   Ok("Hello!")
   * }
   * }}}
   *
   * @param block the action code
   * @return an action
   */
  def apply(block: => Result): Action[AnyContent] = {
    this.apply(_ => block)
  }

}
