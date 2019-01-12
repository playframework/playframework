/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.http

/**
 * Defines common HTTP Content-Type header values, according to the current available Codec.
 */
object ContentTypes extends ContentTypes

/** Defines common HTTP Content-Type header values, according to the current available Codec. */
trait ContentTypes {

  import play.api.mvc.Codec

  /**
   * Content-Type of text.
   */
  def TEXT(implicit codec: Codec) = withCharset(MimeTypes.TEXT)

  /**
   * Content-Type of html.
   */
  def HTML(implicit codec: Codec) = withCharset(MimeTypes.HTML)

  /**
   * Content-Type of xhtml.
   */
  def XHTML(implicit codec: Codec) = withCharset(MimeTypes.XHTML)

  /**
   * Content-Type of xml.
   */
  def XML(implicit codec: Codec) = withCharset(MimeTypes.XML)

  /**
   * Content-Type of css.
   */
  def CSS(implicit codec: Codec) = withCharset(MimeTypes.CSS)

  /**
   * Content-Type of javascript.
   */
  def JAVASCRIPT(implicit codec: Codec) = withCharset(MimeTypes.JAVASCRIPT)

  /**
   * Content-Type of server sent events.
   */
  def EVENT_STREAM(implicit codec: Codec) = withCharset(MimeTypes.EVENT_STREAM)

  /**
   * Content-Type of application cache.
   */
  final val CACHE_MANIFEST = withCharset(MimeTypes.CACHE_MANIFEST)(Codec.utf_8)

  /**
   * Content-Type of json. This content type does not define a charset parameter.
   */
  final val JSON = MimeTypes.JSON

  /**
   * Content-Type of form-urlencoded. This content type does not define a charset parameter.
   */
  final val FORM = MimeTypes.FORM

  /**
   * Content-Type of binary data.
   */
  final val BINARY = MimeTypes.BINARY

  /**
   * @return the `codec` charset appended to `mimeType`
   */
  def withCharset(mimeType: String)(implicit codec: Codec) = s"$mimeType; charset=${codec.charset}"

}

/**
 * Standard HTTP Verbs
 */
object HttpVerbs extends HttpVerbs

/**
 * Standard HTTP Verbs
 */
trait HttpVerbs {
  final val GET = "GET"
  final val POST = "POST"
  final val PUT = "PUT"
  final val PATCH = "PATCH"
  final val DELETE = "DELETE"
  final val HEAD = "HEAD"
  final val OPTIONS = "OPTIONS"
}

/** Common HTTP MIME types */
object MimeTypes extends MimeTypes

/** Common HTTP MIME types */
trait MimeTypes {

  /**
   * Content-Type of text.
   */
  final val TEXT = "text/plain"

  /**
   * Content-Type of html.
   */
  final val HTML = "text/html"

  /**
   * Content-Type of json.
   */
  final val JSON = "application/json"

  /**
   * Content-Type of xml.
   */
  final val XML = "application/xml"

  /**
   * Content-Type of xml.
   */
  final val XHTML = "application/xhtml+xml"

  /**
   * Content-Type of css.
   */
  final val CSS = "text/css"

  /**
   * Content-Type of javascript.
   */
  final val JAVASCRIPT = "application/javascript"

  /**
   * Content-Type of form-urlencoded.
   */
  final val FORM = "application/x-www-form-urlencoded"

  /**
   * Content-Type of server sent events.
   */
  final val EVENT_STREAM = "text/event-stream"

  /**
   * Content-Type of binary data.
   */
  final val BINARY = "application/octet-stream"

  /**
   * Content-Type of application cache.
   */
  final val CACHE_MANIFEST = "text/cache-manifest"

}

/**
 * Defines all standard HTTP status codes, with additional helpers for determining the type of status.
 */
object Status extends Status {
  def isInformational(status: Int): Boolean = status / 100 == 1
  def isSuccessful(status: Int): Boolean = status / 100 == 2
  def isRedirect(status: Int): Boolean = status / 100 == 3
  def isClientError(status: Int): Boolean = status / 100 == 4
  def isServerError(status: Int): Boolean = status / 100 == 5
}

/**
 * Defines all standard HTTP status codes.
 *
 * See <a href="https://tools.ietf.org/html/rfc7231">RFC 7231</a> and <a href="https://tools.ietf.org/html/rfc6585">RFC 6585</a>.
 */
trait Status {

  final val CONTINUE = 100
  final val SWITCHING_PROTOCOLS = 101

  final val OK = 200
  final val CREATED = 201
  final val ACCEPTED = 202
  final val NON_AUTHORITATIVE_INFORMATION = 203
  final val NO_CONTENT = 204
  final val RESET_CONTENT = 205
  final val PARTIAL_CONTENT = 206
  final val MULTI_STATUS = 207

  final val MULTIPLE_CHOICES = 300
  final val MOVED_PERMANENTLY = 301
  final val FOUND = 302
  final val SEE_OTHER = 303
  final val NOT_MODIFIED = 304
  final val USE_PROXY = 305
  final val TEMPORARY_REDIRECT = 307
  final val PERMANENT_REDIRECT = 308

  final val BAD_REQUEST = 400
  final val UNAUTHORIZED = 401
  final val PAYMENT_REQUIRED = 402
  final val FORBIDDEN = 403
  final val NOT_FOUND = 404
  final val METHOD_NOT_ALLOWED = 405
  final val NOT_ACCEPTABLE = 406
  final val PROXY_AUTHENTICATION_REQUIRED = 407
  final val REQUEST_TIMEOUT = 408
  final val CONFLICT = 409
  final val GONE = 410
  final val LENGTH_REQUIRED = 411
  final val PRECONDITION_FAILED = 412
  final val REQUEST_ENTITY_TOO_LARGE = 413
  final val REQUEST_URI_TOO_LONG = 414
  final val UNSUPPORTED_MEDIA_TYPE = 415
  final val REQUESTED_RANGE_NOT_SATISFIABLE = 416
  final val EXPECTATION_FAILED = 417
  final val IM_A_TEAPOT = 418
  final val UNPROCESSABLE_ENTITY = 422
  final val LOCKED = 423
  final val FAILED_DEPENDENCY = 424
  final val UPGRADE_REQUIRED = 426
  final val PRECONDITION_REQUIRED = 428
  final val TOO_MANY_REQUESTS = 429
  final val REQUEST_HEADER_FIELDS_TOO_LARGE = 431
  @deprecated("Use TOO_MANY_REQUESTS instead", "2.6.0")
  final val TOO_MANY_REQUEST = TOO_MANY_REQUESTS

  final val INTERNAL_SERVER_ERROR = 500
  final val NOT_IMPLEMENTED = 501
  final val BAD_GATEWAY = 502
  final val SERVICE_UNAVAILABLE = 503
  final val GATEWAY_TIMEOUT = 504
  final val HTTP_VERSION_NOT_SUPPORTED = 505
  final val INSUFFICIENT_STORAGE = 507
  final val NETWORK_AUTHENTICATION_REQUIRED = 511
}

/** Defines all standard HTTP headers. */
object HeaderNames extends HeaderNames

/** Defines all standard HTTP headers. */
trait HeaderNames {

  final val ACCEPT = "Accept"
  final val ACCEPT_CHARSET = "Accept-Charset"
  final val ACCEPT_ENCODING = "Accept-Encoding"
  final val ACCEPT_LANGUAGE = "Accept-Language"
  final val ACCEPT_RANGES = "Accept-Ranges"
  final val AGE = "Age"
  final val ALLOW = "Allow"
  final val AUTHORIZATION = "Authorization"

  final val CACHE_CONTROL = "Cache-Control"
  final val CONNECTION = "Connection"
  final val CONTENT_DISPOSITION = "Content-Disposition"
  final val CONTENT_ENCODING = "Content-Encoding"
  final val CONTENT_LANGUAGE = "Content-Language"
  final val CONTENT_LENGTH = "Content-Length"
  final val CONTENT_LOCATION = "Content-Location"
  final val CONTENT_MD5 = "Content-MD5"
  final val CONTENT_RANGE = "Content-Range"
  final val CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding"
  final val CONTENT_TYPE = "Content-Type"
  final val COOKIE = "Cookie"

  final val DATE = "Date"

  final val ETAG = "ETag"
  final val EXPECT = "Expect"
  final val EXPIRES = "Expires"

  final val FROM = "From"

  final val HOST = "Host"

  final val IF_MATCH = "If-Match"
  final val IF_MODIFIED_SINCE = "If-Modified-Since"
  final val IF_NONE_MATCH = "If-None-Match"
  final val IF_RANGE = "If-Range"
  final val IF_UNMODIFIED_SINCE = "If-Unmodified-Since"

  final val LAST_MODIFIED = "Last-Modified"
  final val LINK = "Link"
  final val LOCATION = "Location"

  final val MAX_FORWARDS = "Max-Forwards"

  final val PRAGMA = "Pragma"
  final val PROXY_AUTHENTICATE = "Proxy-Authenticate"
  final val PROXY_AUTHORIZATION = "Proxy-Authorization"

  final val RANGE = "Range"
  final val REFERER = "Referer"
  final val RETRY_AFTER = "Retry-After"

  final val SERVER = "Server"

  final val SET_COOKIE = "Set-Cookie"
  final val SET_COOKIE2 = "Set-Cookie2"

  final val TE = "Te"
  final val TRAILER = "Trailer"
  final val TRANSFER_ENCODING = "Transfer-Encoding"

  final val UPGRADE = "Upgrade"
  final val USER_AGENT = "User-Agent"

  final val VARY = "Vary"
  final val VIA = "Via"

  final val WARNING = "Warning"
  final val WWW_AUTHENTICATE = "WWW-Authenticate"

  final val FORWARDED = "Forwarded"
  final val X_FORWARDED_FOR = "X-Forwarded-For"
  final val X_FORWARDED_HOST = "X-Forwarded-Host"
  final val X_FORWARDED_PORT = "X-Forwarded-Port"
  final val X_FORWARDED_PROTO = "X-Forwarded-Proto"

  final val X_REQUESTED_WITH = "X-Requested-With"

  final val ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin"
  final val ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers"
  final val ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age"
  final val ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials"
  final val ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods"
  final val ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers"

  final val ORIGIN = "Origin"
  final val ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method"
  final val ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers"

  final val STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security"

  final val X_FRAME_OPTIONS = "X-Frame-Options"
  final val X_XSS_PROTECTION = "X-XSS-Protection"
  final val X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options"
  final val X_PERMITTED_CROSS_DOMAIN_POLICIES = "X-Permitted-Cross-Domain-Policies"
  final val REFERRER_POLICY = "Referrer-Policy"

  final val CONTENT_SECURITY_POLICY = "Content-Security-Policy"
  final val CONTENT_SECURITY_POLICY_REPORT_ONLY: String = "Content-Security-Policy-Report-Only"
  final val X_CONTENT_SECURITY_POLICY_NONCE_HEADER: String = "X-Content-Security-Policy-Nonce"
}

/**
 * Defines HTTP protocol constants
 */
object HttpProtocol extends HttpProtocol

/**
 * Defines HTTP protocol constants
 */
trait HttpProtocol {
  // Versions
  final val HTTP_1_0 = "HTTP/1.0"
  final val HTTP_1_1 = "HTTP/1.1"

  // Other HTTP protocol values
  final val CHUNKED = "chunked"
}
