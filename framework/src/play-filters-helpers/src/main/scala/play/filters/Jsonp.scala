package play.filters

import play.api.mvc._
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.ContentTypes.{JSON, JAVASCRIPT}
import concurrent.ExecutionContext
import play.api.libs.iteratee.Enumerator

/**
 * Transforms JSON responses into JavaScript responses if there is a `paramName` parameter in the request’s query string.
 *
 * See [[http://www.json-p.org/]] for more information about JSONP.
 *
 * @param paramName Name of the query string parameter containing the callback name.
 * @param codec Codec used to serialize the response body
 * @param ex Execution context to use in case of asynchronous results
 */
class Jsonp(paramName: String = "callback")(implicit codec: Codec, ex: ExecutionContext) extends EssentialFilter {

  def apply(action: EssentialAction) = EssentialAction { request =>
    val resultProducer = action(request)
    request.getQueryString(paramName) match {
      case Some(callback) => resultProducer.map(jsonpify(callback))
      case None => resultProducer
    }
  }

  /**
   * Tries to transform a response into a JavaScript expression.
   * @param callback JavaScript callback name
   * @param result Result to transform
   */
  def jsonpify(callback: String)(result: Result): Result = result match {
    case result: SimpleResult[_] =>
      result.header.headers.get(CONTENT_TYPE) match {
        case Some(ct) if ct == JSON =>
          SimpleResult(
            result.header,
            Enumerator(codec.encode(s"$callback(")) >>> result.body.map(body => result.writeable.transform(body)) >>> Enumerator(codec.encode(");"))
          ).as(JAVASCRIPT)
        case _ => result
      }
    case AsyncResult(result) => AsyncResult(result.map(jsonpify(callback)))
    case _ => result // ChunkedResult is not supported and PlainResult have no body
  }

}
