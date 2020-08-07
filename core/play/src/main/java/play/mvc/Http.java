/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

/** Defines HTTP standard objects. */
public class Http {

  /** Defines all standard HTTP headers. */
  public interface HeaderNames {

    String ACCEPT = "Accept";
    String ACCEPT_CHARSET = "Accept-Charset";
    String ACCEPT_ENCODING = "Accept-Encoding";
    String ACCEPT_LANGUAGE = "Accept-Language";
    String ACCEPT_RANGES = "Accept-Ranges";
    String AGE = "Age";
    String ALLOW = "Allow";
    String AUTHORIZATION = "Authorization";
    String CACHE_CONTROL = "Cache-Control";
    String CONNECTION = "Connection";
    String CONTENT_DISPOSITION = "Content-Disposition";
    String CONTENT_ENCODING = "Content-Encoding";
    String CONTENT_LANGUAGE = "Content-Language";
    String CONTENT_LENGTH = "Content-Length";
    String CONTENT_LOCATION = "Content-Location";
    String CONTENT_MD5 = "Content-MD5";
    String CONTENT_RANGE = "Content-Range";
    String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    String CONTENT_TYPE = "Content-Type";
    String COOKIE = "Cookie";
    String DATE = "Date";
    String ETAG = "ETag";
    String EXPECT = "Expect";
    String EXPIRES = "Expires";
    String FORWARDED = "Forwarded";
    String FROM = "From";
    String HOST = "Host";
    String IF_MATCH = "If-Match";
    String IF_MODIFIED_SINCE = "If-Modified-Since";
    String IF_NONE_MATCH = "If-None-Match";
    String IF_RANGE = "If-Range";
    String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
    String LAST_MODIFIED = "Last-Modified";
    String LINK = "Link";
    String LOCATION = "Location";
    String MAX_FORWARDS = "Max-Forwards";
    String PRAGMA = "Pragma";
    String PROXY_AUTHENTICATE = "Proxy-Authenticate";
    String PROXY_AUTHORIZATION = "Proxy-Authorization";
    String RANGE = "Range";
    String REFERER = "Referer";
    String RETRY_AFTER = "Retry-After";
    String SERVER = "Server";
    String SET_COOKIE = "Set-Cookie";
    String SET_COOKIE2 = "Set-Cookie2";
    String TE = "Te";
    String TRAILER = "Trailer";
    String TRANSFER_ENCODING = "Transfer-Encoding";
    String UPGRADE = "Upgrade";
    String USER_AGENT = "User-Agent";
    String VARY = "Vary";
    String VIA = "Via";
    String WARNING = "Warning";
    String WWW_AUTHENTICATE = "WWW-Authenticate";
    String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
    String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    String ORIGIN = "Origin";
    String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
    String X_FORWARDED_FOR = "X-Forwarded-For";
    String X_FORWARDED_HOST = "X-Forwarded-Host";
    String X_FORWARDED_PORT = "X-Forwarded-Port";
    String X_FORWARDED_PROTO = "X-Forwarded-Proto";
    String X_REQUESTED_WITH = "X-Requested-With";
    String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
    String X_FRAME_OPTIONS = "X-Frame-Options";
    String X_XSS_PROTECTION = "X-XSS-Protection";
    String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    String X_PERMITTED_CROSS_DOMAIN_POLICIES = "X-Permitted-Cross-Domain-Policies";
    String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    String CONTENT_SECURITY_POLICY_REPORT_ONLY = "Content-Security-Policy-Report-Only";
    String X_CONTENT_SECURITY_POLICY_NONCE_HEADER = "X-Content-Security-Policy-Nonce";
    String REFERRER_POLICY = "Referrer-Policy";
  }

  /**
   * Defines all standard HTTP status codes.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231">RFC 7231</a> and <a
   *     href="https://tools.ietf.org/html/rfc6585">RFC 6585</a>
   */
  public interface Status {
    int CONTINUE = 100;
    int SWITCHING_PROTOCOLS = 101;

    int OK = 200;
    int CREATED = 201;
    int ACCEPTED = 202;
    int NON_AUTHORITATIVE_INFORMATION = 203;
    int NO_CONTENT = 204;
    int RESET_CONTENT = 205;
    int PARTIAL_CONTENT = 206;
    int MULTI_STATUS = 207;

    int MULTIPLE_CHOICES = 300;
    int MOVED_PERMANENTLY = 301;
    int FOUND = 302;
    int SEE_OTHER = 303;
    int NOT_MODIFIED = 304;
    int USE_PROXY = 305;
    int TEMPORARY_REDIRECT = 307;
    int PERMANENT_REDIRECT = 308;

    int BAD_REQUEST = 400;
    int UNAUTHORIZED = 401;
    int PAYMENT_REQUIRED = 402;
    int FORBIDDEN = 403;
    int NOT_FOUND = 404;
    int METHOD_NOT_ALLOWED = 405;
    int NOT_ACCEPTABLE = 406;
    int PROXY_AUTHENTICATION_REQUIRED = 407;
    int REQUEST_TIMEOUT = 408;
    int CONFLICT = 409;
    int GONE = 410;
    int LENGTH_REQUIRED = 411;
    int PRECONDITION_FAILED = 412;
    int REQUEST_ENTITY_TOO_LARGE = 413;
    int REQUEST_URI_TOO_LONG = 414;
    int UNSUPPORTED_MEDIA_TYPE = 415;
    int REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    int EXPECTATION_FAILED = 417;
    int IM_A_TEAPOT = 418;
    int UNPROCESSABLE_ENTITY = 422;
    int LOCKED = 423;
    int FAILED_DEPENDENCY = 424;
    int UPGRADE_REQUIRED = 426;

    // See https://tools.ietf.org/html/rfc6585 for the following statuses
    int PRECONDITION_REQUIRED = 428;
    int TOO_MANY_REQUESTS = 429;
    int REQUEST_HEADER_FIELDS_TOO_LARGE = 431;

    int INTERNAL_SERVER_ERROR = 500;
    int NOT_IMPLEMENTED = 501;
    int BAD_GATEWAY = 502;
    int SERVICE_UNAVAILABLE = 503;
    int GATEWAY_TIMEOUT = 504;
    int HTTP_VERSION_NOT_SUPPORTED = 505;
    int INSUFFICIENT_STORAGE = 507;

    // See https://tools.ietf.org/html/rfc6585#section-6
    int NETWORK_AUTHENTICATION_REQUIRED = 511;
  }

  /** Common HTTP MIME types */
  public interface MimeTypes {

    /** Content-Type of text. */
    String TEXT = "text/plain";

    /** Content-Type of html. */
    String HTML = "text/html";

    /** Content-Type of json. */
    String JSON = "application/json";

    /** Content-Type of xml. */
    String XML = "application/xml";

    /** Content-Type of xhtml. */
    String XHTML = "application/xhtml+xml";

    /** Content-Type of css. */
    String CSS = "text/css";

    /** Content-Type of javascript. */
    String JAVASCRIPT = "application/javascript";

    /** Content-Type of form-urlencoded. */
    String FORM = "application/x-www-form-urlencoded";

    /** Content-Type of server sent events. */
    String EVENT_STREAM = "text/event-stream";

    /** Content-Type of binary data. */
    String BINARY = "application/octet-stream";
  }

  /** Standard HTTP Verbs */
  public interface HttpVerbs {
    String GET = "GET";
    String POST = "POST";
    String PUT = "PUT";
    String PATCH = "PATCH";
    String DELETE = "DELETE";
    String HEAD = "HEAD";
    String OPTIONS = "OPTIONS";
  }
}
