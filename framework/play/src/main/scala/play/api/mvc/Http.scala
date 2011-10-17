package play.api.mvc {

  import play.api._
  import play.core.Iteratee._

  import scala.annotation._

  @implicitNotFound("Cannot find any HTTP Request Header here")
  trait RequestHeader {

    def uri: String
    def path: String
    def method: String
    def queryString: Map[String, Seq[String]]
    def headers: Headers
    def cookies: Cookies
    def username: Option[String]

    lazy val session: Session = Session.decodeFromCookie(cookies.get(Session.SESSION_COOKIE_NAME))
    lazy val flash: Flash = Flash.decodeFromCookie(cookies.get(Flash.FLASH_COOKIE_NAME))
    lazy val rawQueryString = uri.split('?').drop(1).mkString("?")

    override def toString = {
      method + " " + uri
    }

  }

  @implicitNotFound("Cannot find any HTTP Request here")
  trait Request[+A] extends RequestHeader {
    def body: A

  }

  trait Response {
    def handle(result: Result): Unit
  }

  case class Call(method: String, url: String) extends play.mvc.Call {
    override def toString = url
  }

  trait Headers {
    def get(key: String): Option[String] = getAll(key).headOption
    def apply(key: String): String = get(key).getOrElse(scala.sys.error("Header doesn't exist"))
    def getAll(key: String): Seq[String]
  }

  case class Session(data: Map[String, String] = Map.empty[String, String]) {
    def get(key: String) = data.get(key)
    def isEmpty: Boolean = data.isEmpty
    def +(kv: (String, String)) = copy(data + kv)
    def -(key: String) = copy(data - key)
    def apply(key: String) = data(key)
  }

  object Session {

    val SESSION_COOKIE_NAME = "PLAY_SESSION"
    val blankSession = new Session

    def encode(session: Session): String = {
      java.net.URLEncoder.encode(session.data.filterNot(_._1.contains(":")).map(d => d._1 + ":" + d._2).mkString("\u0000"))
    }

    def decode(data: String): Session = {
      try {
        Option(data.trim).filterNot(_.isEmpty).map { data =>
          Session(java.net.URLDecoder.decode(data).split("\u0000").map(_.split(":")).map(p => p(0) -> p.drop(1).mkString(":")).toMap)
        }.getOrElse(blankSession)
      } catch {
        // fail gracefully is the session cookie is corrupted
        case _ => blankSession
      }
    }

    def encodeAsCookie(data: Session): Cookie = {
      Cookie(SESSION_COOKIE_NAME, encode(data))
    }

    def decodeFromCookie(sessionCookie: Option[Cookie]): Session = {
      sessionCookie.filter(_.name == SESSION_COOKIE_NAME).map(c => decode(c.value)).getOrElse(blankSession)
    }

  }

  case class Flash(data: Map[String, String] = Map.empty[String, String]) {
    def get(key: String) = data.get(key)
    def isEmpty: Boolean = data.isEmpty
    def +(kv: (String, String)) = copy(data + kv)
    def -(key: String) = copy(data - key)
    def apply(key: String) = data(key)
  }

  object Flash {

    val FLASH_COOKIE_NAME = "PLAY_FLASH"
    val blankFlash = new Flash

    def encode(flash: Flash): String = {
      java.net.URLEncoder.encode(flash.data.filterNot(_._1.contains(":")).map(d => d._1 + ":" + d._2).mkString("\u0000"))
    }

    def decode(data: String): Flash = {
      try {
        Option(data.trim).filterNot(_.isEmpty).map { data =>
          Flash(java.net.URLDecoder.decode(data).split("\u0000").map(_.split(":")).map(p => p(0) -> p.drop(1).mkString(":")).toMap)
        }.getOrElse(blankFlash)
      } catch {
        // fail gracefully is the flash cookie is corrupted
        case _ => blankFlash
      }
    }

    def encodeAsCookie(data: Flash): Cookie = {
      Cookie(FLASH_COOKIE_NAME, encode(data))
    }

    def decodeFromCookie(flashCookie: Option[Cookie]): Flash = {
      flashCookie.filter(_.name == FLASH_COOKIE_NAME).map(c => decode(c.value)).getOrElse(blankFlash)
    }

  }

  case class Cookie(name: String, value: String, maxAge: Int = -1, path: String = "/", domain: Option[String] = None, secure: Boolean = false, httpOnly: Boolean = true)

  trait Cookies {
    def get(name: String): Option[Cookie]
    def apply(name: String): Cookie = get(name).getOrElse(scala.sys.error("Cookie doesn't exist"))
  }

  object Cookies {

    import scala.collection.JavaConverters._

    // We use netty here but just as an API to handle cookies encoding
    import org.jboss.netty.handler.codec.http.{ CookieEncoder, CookieDecoder, DefaultCookie }

    def encode(cookies: Seq[Cookie], discard: Seq[String] = Nil): String = {
      val encoder = new CookieEncoder(true)
      cookies.foreach { c =>
        encoder.addCookie {
          val nc = new DefaultCookie(c.name, c.value)
          nc.setMaxAge(c.maxAge)
          nc.setPath(c.path)
          c.domain.map(nc.setDomain(_))
          nc.setSecure(c.secure)
          nc.setHttpOnly(c.httpOnly)
          nc
        }
      }
      discard.foreach { n =>
        encoder.addCookie {
          val nc = new DefaultCookie(n, "")
          nc.setMaxAge(0)
          nc
        }
      }
      encoder.encode()
    }

    def decode(cookieHeader: String): Seq[Cookie] = {
      new CookieDecoder().decode(cookieHeader).asScala.map { c =>
        Cookie(c.getName, c.getValue, c.getMaxAge, Option(c.getPath).getOrElse("/"), Option(c.getDomain), c.isSecure, c.isHttpOnly)
      }.toSeq
    }

    def merge(cookieHeader: String, cookies: Seq[Cookie], discard: Seq[String] = Nil): String = {
      encode(decode(cookieHeader) ++ cookies, discard)
    }

  }

}

package play.api.http {

  object Status extends Status

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

    val INTERNAL_SERVER_ERROR = 500
    val NOT_IMPLEMENTED = 501
    val BAD_GATEWAY = 502
    val SERVICE_UNAVAILABLE = 503
    val GATEWAY_TIMEOUT = 504
    val HTTP_VERSION_NOT_SUPPORTED = 505

  }

  object HeaderNames extends HeaderNames

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

}
