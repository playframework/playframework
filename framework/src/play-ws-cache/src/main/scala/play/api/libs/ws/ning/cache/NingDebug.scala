package play.api.libs.ws.ning.cache

import com.ning.http.client._
import play.api.libs.ws.ning.NingUtilities

trait NingDebug extends NingUtilities {

  def debug(cfg: AsyncHttpClientConfig): String = {
    s"AsyncHttpClientConfig(requestFilters = ${cfg.getRequestFilters})"
  }

  def debug(request: Request): String = {
    Option(request).map { r =>
      s"Request(${r.getMethod} ${r.getUrl})"
    }.getOrElse("null")
  }

  def debug(response: Response): String = {
    Option(response).map {
      case cr: CacheableResponse =>
        cr.toString
      case r =>
        s"Response(${r.getStatusCode} ${r.getStatusText})"
    }.getOrElse("null")
  }

  def debug(responseStatus: HttpResponseStatus): String = {
    Option(responseStatus).map {
      case cs: CacheableHttpResponseStatus =>
        cs.toString
      case s =>
        s"HttpResponseStatus(${s.getProtocolName} ${s.getStatusCode} ${s.getStatusText})"
    }.getOrElse("null")
  }

  def debug(responseHeaders: HttpResponseHeaders): String = {
    Option(responseHeaders).map {
      case crh: CacheableHttpResponseHeaders =>
        crh.toString
      case rh =>
        s"HttpResponseHeaders(${ningHeadersToMap(rh.getHeaders)})"
    }.getOrElse("null")
  }

  def debug(bodyParts: java.util.List[HttpResponseBodyPart]): String = {
    import collection.JavaConverters._
    bodyParts.asScala.map(debug).toString()
  }

  def debug[T](handler: AsyncHandler[T]): String = {
    s"AsyncHandler($handler)"
  }

  def debug[T](ctx: com.ning.http.client.filter.FilterContext[T]): String = {
    s"FilterContext(request = ${debug(ctx.getRequest)}}, responseStatus = ${debug(ctx.getResponseStatus)}}, responseHeaders = ${debug(ctx.getResponseHeaders)}})"
  }

  def debug(bodyPart: HttpResponseBodyPart): String = {
    bodyPart match {
      case cbp: CacheableHttpResponseBodyPart =>
        cbp.toString
      case otherBodyPart =>
        s"HttpResponseBodyPart(length = ${otherBodyPart.length()}})"
    }
  }
}
