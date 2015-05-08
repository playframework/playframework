package play.api.libs.ws.ning.cache

import java.net.URI

import com.ning.http.client.Request

case class CacheKey(method: String, uri: URI) {
  override def toString: String = method + " " + uri.toString
}

object CacheKey {
  def apply(request: Request): CacheKey = {
    require(request != null)
    CacheKey(request.getMethod, request.getUri.toJavaNetURI)
  }
}