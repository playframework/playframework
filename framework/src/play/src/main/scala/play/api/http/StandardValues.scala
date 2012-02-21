package play.api.http

/**
 * Defines common HTTP Content-Type.
 */
object ContentTypes extends ContentTypes

/** Defines common HTTP Content-Type. */
trait ContentTypes {

  import play.api.mvc.Codec

  /**
   * Content-Type of text according the implicit codec value.
   */
  def TEXT(implicit codec: Codec): String = ("text/plain; charset=" + codec.charset)

  /**
   * Content-Type of html according the implicit codec value.
   */
  def HTML(implicit codec: Codec): String = ("text/html; charset=" + codec.charset)

  /**
   * Content-Type of json according the implicit codec value.
   */
  def JSON(implicit codec: Codec): String = ("application/json; charset=" + codec.charset)

  /**
   * Content-Type of xml according the implicit codec value.
   */
  def XML(implicit codec: Codec): String = ("text/xml; charset=" + codec.charset)

  /**
   * Content-Type of css according the implicit codec value.
   */
  def CSS(implicit codec: Codec): String = ("text/css; charset=" + codec.charset)

  /**
   * Content-Type of javascript according the implicit codec value.
   */
  def JAVASCRIPT(implicit codec: Codec): String = ("text/javascript; charset=" + codec.charset)

  /**
   * Content-Type of form-urlencoded according the implicit codec value.
   */
  def FORM(implicit codec: Codec): String = ("application/x-www-form-urlencoded; charset=" + codec.charset)

  /**
   * Content-Type of server sent events according the implicit codec value
   */
  def EVENT_STREAM(implicit codec: Codec): String = ("text/event-stream; charset=" + codec.charset)

  /**
   * Content-Type of binary data.
   */
  val BINARY = "application/octet-stream"

}

/**
 * Defines all standard HTTP Status.
 */
object Status extends Status

/**
 * Defines all standard HTTP status codes.
 */
trait Status {

  val CONTINUE = 100
  val SWITCHING_PROTOCOLS = 101

  val OK = 200
  val CREATED = 201
  val ACCEPTED = 202
  val NON_AUTHORITATIVE_INFORMATION = 203
  val NO_CONTENT = 204
  val RESET_CONTENT = 205
  val PARTIAL_CONTENT = 206

  val MULTIPLE_CHOICES = 300
  val MOVED_PERMANENTLY = 301
  val FOUND = 302
  val SEE_OTHER = 303
  val NOT_MODIFIED = 304
  val USE_PROXY = 305
  val TEMPORARY_REDIRECT = 307

  val BAD_REQUEST = 400
  val UNAUTHORIZED = 401
  val PAYMENT_REQUIRED = 402
  val FORBIDDEN = 403
  val NOT_FOUND = 404
  val METHOD_NOT_ALLOWED = 405
  val NOT_ACCEPTABLE = 406
  val PROXY_AUTHENTICATION_REQUIRED = 407
  val REQUEST_TIMEOUT = 408
  val CONFLICT = 409
  val GONE = 410
  val LENGTH_REQUIRED = 411
  val PRECONDITION_FAILED = 412
  val REQUEST_ENTITY_TOO_LARGE = 413
  val REQUEST_URI_TOO_LONG = 414
  val UNSUPPORTED_MEDIA_TYPE = 415
  val REQUESTED_RANGE_NOT_SATISFIABLE = 416
  val EXPECTATION_FAILED = 417
  val TOO_MANY_REQUEST = 429

  val INTERNAL_SERVER_ERROR = 500
  val NOT_IMPLEMENTED = 501
  val BAD_GATEWAY = 502
  val SERVICE_UNAVAILABLE = 503
  val GATEWAY_TIMEOUT = 504
  val HTTP_VERSION_NOT_SUPPORTED = 505

}

/** Defines all standard HTTP headers. */
object HeaderNames extends HeaderNames

/** Defines all standard HTTP headers. */
trait HeaderNames {

  val ACCEPT = "Accept"
  val ACCEPT_CHARSET = "Accept-Charset"
  val ACCEPT_ENCODING = "Accept-Encoding"
  val ACCEPT_LANGUAGE = "Accept-Language"
  val ACCEPT_RANGES = "Accept-Ranges"
  val AGE = "Age"
  val ALLOW = "Allow"
  val AUTHORIZATION = "Authorization"

  val CACHE_CONTROL = "Cache-Control"
  val CONNECTION = "Connection"
  val CONTENT_DISPOSITION = "Content-Disposition"
  val CONTENT_ENCODING = "Content-Encoding"
  val CONTENT_LANGUAGE = "Content-Language"
  val CONTENT_LENGTH = "Content-Length"
  val CONTENT_LOCATION = "Content-Location"
  val CONTENT_MD5 = "Content-MD5"
  val CONTENT_RANGE = "Content-Range"
  val CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding"
  val CONTENT_TYPE = "Content-Type"
  val COOKIE = "Cookie"

  val DATE = "Date"

  val ETAG = "Etag"
  val EXPECT = "Expect"
  val EXPIRES = "Expires"

  val FROM = "From"

  val HOST = "Host"

  val IF_MATCH = "If-Match"
  val IF_MODIFIED_SINCE = "If-Modified-Since"
  val IF_NONE_MATCH = "If-None-Match"
  val IF_RANGE = "If-Range"
  val IF_UNMODIFIED_SINCE = "If-Unmodified-Since"

  val LAST_MODIFIED = "Last-Modified"
  val LOCATION = "Location"

  val MAX_FORWARDS = "Max-Forwards"

  val PRAGMA = "Pragma"
  val PROXY_AUTHENTICATE = "Proxy-Authenticate"
  val PROXY_AUTHORIZATION = "Proxy-Authorization"

  val RANGE = "Range"
  val REFERER = "Referer"
  val RETRY_AFTER = "Retry-After"

  val SERVER = "Server"

  val SET_COOKIE = "Set-Cookie"
  val SET_COOKIE2 = "Set-Cookie2"

  val TE = "Te"
  val TRAILER = "Trailer"
  val TRANSFER_ENCODING = "Transfer-Encoding"

  val UPGRADE = "Upgrade"
  val USER_AGENT = "User-Agent"

  val VARY = "Vary"
  val VIA = "Via"

  val WARNING = "Warning"
  val WWW_AUTHENTICATE = "WWW-Authenticate"

}

