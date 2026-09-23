/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import java.util.Locale

import scala.annotation.implicitNotFound

import play.api.http.HeaderNames
import play.api.http.MediaRange
import play.api.http.MediaType
import play.api.i18n.Lang
import play.api.i18n.Messages
import play.api.libs.typedmap.TypedEntry
import play.api.libs.typedmap.TypedKey
import play.api.libs.typedmap.TypedMap
import play.api.mvc.request._

/**
 * The HTTP request header. Note that it doesn't contain the request body yet.
 */
@implicitNotFound("Cannot find any HTTP Request Header here")
trait RequestHeader {
  top =>

  /** Metadata about the network connection directly terminating at Play. */
  def transport: TransportConnection

  /** Return a copy with different direct transport metadata. */
  def withTransport(newTransport: TransportConnection): RequestHeader =
    new RequestHeaderImpl(
      remote,
      method,
      target,
      version,
      headers,
      attrs,
      newTransport,
      clientCertificate,
      scheme,
      authority,
      xForwardedClientCertificates
    )

  /** The effective X.509 client certificate selected for this request, if present. */
  def clientCertificate: Option[ClientCertificateInfo]

  /** Return a copy with different effective client certificate information. */
  def withClientCertificate(newClientCertificate: Option[ClientCertificateInfo]): RequestHeader =
    new RequestHeaderImpl(
      remote,
      method,
      target,
      version,
      headers,
      attrs,
      transport,
      newClientCertificate,
      scheme,
      authority,
      xForwardedClientCertificates
    )

  /** Accepted `X-Forwarded-Client-Cert` assertions in client-to-Play order. */
  def xForwardedClientCertificates: Vector[XForwardedClientCert]

  /** Return a copy with a different ordered sequence of accepted XFCC assertions. */
  def withXForwardedClientCertificates(
      newXForwardedClientCertificates: Seq[XForwardedClientCert]
  ): RequestHeader = {
    require(newXForwardedClientCertificates != null, "The XFCC assertion sequence must not be null")
    new RequestHeaderImpl(
      remote,
      method,
      target,
      version,
      headers,
      attrs,
      transport,
      clientCertificate,
      scheme,
      authority,
      newXForwardedClientCertificates.toVector
    )
  }

  /**
   * The normalized effective request scheme.
   *
   * This is derived initially from direct transport TLS and may then be replaced by trusted forwarding metadata.
   */
  def scheme: Scheme

  /** Return a copy with a different effective request scheme. */
  def withScheme(newScheme: Scheme): RequestHeader =
    new RequestHeaderImpl(
      remote,
      method,
      target,
      version,
      headers,
      attrs,
      transport,
      clientCertificate,
      newScheme,
      authority,
      xForwardedClientCertificates
    )

  /**
   * The normalized effective request authority, if the request has one.
   *
   * [[host]], [[domain]], and the exposed `Host` header are derived from this value.
   */
  def authority: Option[RequestAuthority]

  /**
   * Return a copy with a different effective request authority.
   *
   * This is the only copy operation that changes the canonical `Host` header.
   */
  def withAuthority(newAuthority: Option[RequestAuthority]): RequestHeader =
    new RequestHeaderImpl(
      remote,
      method,
      target,
      version,
      headers,
      attrs,
      transport,
      clientCertificate,
      scheme,
      newAuthority,
      xForwardedClientCertificates
    )

  /**
   * The selected remote-node metadata for this request.
   */
  def remote: RemoteInfo

  /** Return a copy with different selected remote-node metadata. */
  def withRemote(newRemote: RemoteInfo): RequestHeader =
    new RequestHeaderImpl(
      newRemote,
      method,
      target,
      version,
      headers,
      attrs,
      transport,
      clientCertificate,
      scheme,
      authority,
      xForwardedClientCertificates
    )

  /**
   * The request id. The request id is stored as an attribute indexed by [[play.api.mvc.request.RequestAttrKey.Id]].
   */
  final def id: Long = attrs(RequestAttrKey.Id)

  /**
   * The HTTP method.
   */
  def method: String

  /**
   * Return a new copy of the request with its method changed.
   */
  def withMethod(newMethod: String): RequestHeader =
    new RequestHeaderImpl(
      remote,
      newMethod,
      target,
      version,
      headers,
      attrs,
      transport,
      clientCertificate,
      scheme,
      authority,
      xForwardedClientCertificates
    )

  /**
   * The target of the HTTP request, i.e. the URI or path that was
   * given on the first line of the request.
   */
  def target: RequestTarget

  /**
   * Return a new copy of the request with its target changed.
   *
   * This operation preserves the effective [[scheme]] and [[authority]].
   */
  def withTarget(newTarget: RequestTarget): RequestHeader =
    new RequestHeaderImpl(
      remote,
      method,
      newTarget,
      version,
      headers,
      attrs,
      transport,
      clientCertificate,
      scheme,
      authority,
      xForwardedClientCertificates
    )

  /**
   * The complete request URI, containing both path and query string.
   * The URI is what was on the status line after the request method.
   * E.g. in "GET /foo/bar?q=s HTTP/1.1" the URI should be /foo/bar?q=s.
   * It could be absolute, some clients send absolute URLs, especially proxies,
   * e.g. http://www.example.org/foo/bar?q=s.
   *
   * This method delegates to `target.uriString`.
   */
  final def uri: String = target.uriString

  /**
   * The URI path. This method delegates to `target.path`.
   */
  final def path: String = target.path

  /**
   * The HTTP version.
   */
  def version: String

  /**
   * Return a new copy of the request with its HTTP version changed.
   */
  def withVersion(newVersion: String): RequestHeader =
    new RequestHeaderImpl(
      remote,
      method,
      target,
      newVersion,
      headers,
      attrs,
      transport,
      clientCertificate,
      scheme,
      authority,
      xForwardedClientCertificates
    )

  /**
   * The parsed query string. This method delegates to `target.queryMap`.
   */
  final def queryString: Map[String, Seq[String]] = target.queryMap

  /**
   * The HTTP headers.
   */
  def headers: Headers

  /**
   * Return a new copy of the request with its HTTP headers changed.
   *
   * This operation preserves the effective [[authority]]. A missing `Host` field is restored from that authority. A
   * conflicting or duplicate `Host` field is rejected; use [[withAuthority]] to change the authority explicitly.
   */
  def withHeaders(newHeaders: Headers): RequestHeader =
    new RequestHeaderImpl(
      remote,
      method,
      target,
      version,
      RequestHeader.validateReplacementHeaders(newHeaders, authority),
      attrs,
      transport,
      clientCertificate,
      scheme,
      authority,
      xForwardedClientCertificates
    )

  /** Whether the effective request scheme is HTTPS. */
  final def secure: Boolean = scheme.isSecure

  /**
   * A map of typed attributes associated with the request.
   */
  def attrs: TypedMap

  /**
   * Create a new version of this object with the given attributes attached to it.
   * This replaces any existing attributes.
   *
   * @param newAttrs The new attributes to add.
   * @return The new version of this object with the attributes attached.
   */
  def withAttrs(newAttrs: TypedMap): RequestHeader =
    new RequestHeaderImpl(
      remote,
      method,
      target,
      version,
      headers,
      newAttrs,
      transport,
      clientCertificate,
      scheme,
      authority,
      xForwardedClientCertificates
    )

  /**
   * Create a new versions of this object with the given attribute attached to it.
   *
   * @param key The new attribute key.
   * @param value  The attribute value.
   * @tparam A The type of value.
   * @return The new version of this object with the new attribute.
   */
  def addAttr[A](key: TypedKey[A], value: A): RequestHeader =
    withAttrs(attrs.updated(key, value))

  /**
   * Create a new versions of this object with the given attribute attached to it.
   *
   * @param e1 The new attribute.
   * @return The new version of this object with the new attribute.
   */
  def addAttrs(e1: TypedEntry[?]): RequestHeader = withAttrs(attrs.updated(e1))

  /**
   * Create a new versions of this object with the given attributes attached to it.
   *
   * @param e1 The first new attribute.
   * @param e2 The second new attribute.
   * @return The new version of this object with the new attributes.
   */
  def addAttrs(e1: TypedEntry[?], e2: TypedEntry[?]): RequestHeader = withAttrs(attrs.updated(e1, e2))

  /**
   * Create a new versions of this object with the given attributes attached to it.
   *
   * @param e1 The first new attribute.
   * @param e2 The second new attribute.
   * @param e3 The third new attribute.
   * @return The new version of this object with the new attributes.
   */
  def addAttrs(e1: TypedEntry[?], e2: TypedEntry[?], e3: TypedEntry[?]): RequestHeader = withAttrs(
    attrs.updated(e1, e2, e3)
  )

  /**
   * Create a new versions of this object with the given attributes attached to it.
   *
   * @param entries The new attributes.
   * @return The new version of this object with the new attributes.
   */
  def addAttrs(entries: TypedEntry[?]*): RequestHeader =
    withAttrs(attrs.updated(entries*))

  /**
   * Create a new versions of this object with the given attribute removed.
   *
   * @param key The key of the attribute to remove.
   * @return The new version of this object with the attribute removed.
   */
  def removeAttr(key: TypedKey[?]): RequestHeader =
    withAttrs(attrs.removed(key))

  // -- Computed

  /**
   * Helper method to access a queryString parameter. This method delegates to `target.getQueryParameter(key)`.
   *
   * @return The query parameter's value if the parameter is present
   *         and there is only one value. If the parameter is absent
   *         or there is more than one value for that parameter then
   *         `None` is returned.
   */
  def getQueryString(key: String): Option[String] = target.getQueryParameter(key)

  /**
   * True if this request has a body, so we know if we should trigger body parsing. The base implementation simply
   * checks for the Content-Length or Transfer-Encoding headers, but subclasses (such as fake requests) may return
   * true in other cases so the headers need not be updated to reflect the body.
   */
  def hasBody: Boolean = headers.hasBody

  /** The normalized effective request authority, including its port when present. */
  final def host: String = authority.fold("")(_.render)

  /**
   * The HTTP domain. This is the request's [[host]] without its port. Brackets
   * around an IPv6 address are preserved so the result can be used in a URI.
   */
  final def domain: String = authority.fold("")(_.host.render)

  /**
   * The Request Langs extracted from the Accept-Language header and sorted by preference (preferred first).
   */
  lazy val acceptLanguages: Seq[play.api.i18n.Lang] = {
    val langs =
      RequestHeader.acceptHeader(headers, HeaderNames.ACCEPT_LANGUAGE).map(item => (item._1, Lang.get(item._2)))
    langs.sortWith((a, b) => a._1 > b._1).flatMap(_._2)
  }

  /**
   * @return The media types list of the request’s Accept header, sorted by preference (preferred first).
   */
  lazy val acceptedTypes: Seq[play.api.http.MediaRange] = {
    headers.get(HeaderNames.ACCEPT).toSeq.flatMap(MediaRange.parse.apply)
  }

  /**
   * Check if this request accepts a given media type.
   *
   * @return true if `mimeType` matches the Accept header, otherwise false
   */
  def accepts(mimeType: String): Boolean = {
    acceptedTypes.isEmpty || acceptedTypes.exists(_.accepts(mimeType))
  }

  /**
   * The HTTP cookies. The request's cookies are stored in an attribute indexed by
   * [[play.api.mvc.request.RequestAttrKey.Cookies]]. The attribute uses a Cell to store the cookies,
   * to allow them to be evaluated on-demand.
   */
  def cookies: Cookies = attrs.get(RequestAttrKey.Cookies).map(_.value).getOrElse(Cookies(Seq.empty))

  /**
   * Parses the `Session` cookie and returns the `Session` data. The request's session cookie is stored in an attribute indexed by
   * [[play.api.mvc.request.RequestAttrKey.Session]]. The attribute uses a [[play.api.mvc.request.Cell]] to store the session cookie, to allow it to be evaluated on-demand.
   */
  def session: Session = attrs.get(RequestAttrKey.Session).map(_.value).getOrElse(Session(Map.empty))

  /**
   * Parses the `Flash` cookie and returns the `Flash` data. The request's flash cookie is stored in an attribute indexed by
   * [[play.api.mvc.request.RequestAttrKey.Flash]]. The attribute uses a [[play.api.mvc.request.Cell]] to store the flash, to allow it to be evaluated on-demand.
   */
  def flash: Flash = attrs.get(RequestAttrKey.Flash).map(_.value).getOrElse(Flash(Map.empty))

  /**
   * Returns the raw query string. This method delegates to `target.queryString`.
   */
  def rawQueryString: String = target.queryString

  /**
   * The media type of this request.  Same as contentType, except returns a fully parsed media type with parameters.
   */
  lazy val mediaType: Option[MediaType] = headers.get(HeaderNames.CONTENT_TYPE).flatMap(MediaType.parse.apply)

  /**
   * Returns the value of the Content-Type header (without the parameters (eg charset))
   */
  lazy val contentType: Option[String] = mediaType.map(mt => mt.mediaType + "/" + mt.mediaSubType)

  /**
   * Returns the charset of the request for text-based body
   */
  lazy val charset: Option[String] = for {
    mt      <- mediaType
    param   <- mt.parameters.find(_._1.equalsIgnoreCase("charset"))
    charset <- param._2
  } yield charset

  /**
   * Attach a body to this header.
   *
   * @param body The body to attach.
   * @tparam A The type of the body.
   * @return A new request with the body attached to the header.
   */
  def withBody[A](body: A): Request[A] =
    new RequestImpl[A](
      remote,
      method,
      target,
      version,
      headers,
      attrs,
      body,
      transport,
      clientCertificate,
      scheme,
      authority,
      xForwardedClientCertificates
    )

  /**
   * Create a new versions of this object with the given transient language set.
   * The transient language will be taken into account when using [[play.api.i18n.MessagesApi.preferred()]] (It will take precedence over any other language).
   *
   * @param lang The language to use.
   * @return The new version of this object with the given transient language set.
   */
  def withTransientLang(lang: Lang): RequestHeader =
    addAttr(Messages.Attrs.CurrentLang, lang)

  /**
   * Create a new versions of this object with the given transient language set.
   * The transient language will be taken into account when using [[play.api.i18n.MessagesApi.preferred()]] (It will take precedence over any other language).
   *
   * @param code The language to use.
   * @return The new version of this object with the given transient language set.
   */
  def withTransientLang(code: String): RequestHeader =
    withTransientLang(Lang(code))

  /**
   * Create a new versions of this object with the given transient language set.
   * The transient language will be taken into account when using [[play.api.i18n.MessagesApi.preferred()]] (It will take precedence over any other language).
   *
   * @param locale The language to use.
   * @return The new version of this object with the given transient language set.
   */
  def withTransientLang(locale: Locale): RequestHeader =
    withTransientLang(Lang(locale))

  /**
   * Create a new versions of this object with the given transient language removed.
   *
   * @return The new version of this object with the transient language removed.
   */
  def withoutTransientLang(): RequestHeader =
    removeAttr(Messages.Attrs.CurrentLang)

  /**
   * The transient language will be taken into account when using [[play.api.i18n.MessagesApi.preferred()]] (It will take precedence over any other language).
   *
   * @return The current transient language of this request.
   */
  def transientLang(): Option[Lang] =
    attrs.get(Messages.Attrs.CurrentLang)

  override def toString: String = {
    method + " " + uri
  }

  def asJava: play.mvc.Http.RequestHeader = new play.core.j.RequestHeaderImpl(this)
}

object RequestHeader {
  private val UriScheme = """^([A-Za-z][A-Za-z0-9+.-]*):""".r

  /** Recognized HTTP(S) absolute-form metadata before authority parsing. */
  private[play] final case class AbsoluteRequestTarget(
      scheme: Scheme,
      authority: Option[String]
  )

  /** Direct request-target metadata selected before trusted forwarding is applied. */
  private[play] final case class InitialRequestTarget(
      scheme: Option[Scheme],
      authority: Option[RequestAuthority]
  )

  /** Derive the effective scheme from the transport terminating directly at Play. */
  private[play] def initialScheme(transport: TransportConnection): Scheme = {
    if (transport.tls.isDefined) Scheme.Https else Scheme.Http
  }

  /** Derive and validate request-target metadata before trusted forwarding metadata is applied. */
  private[play] def initialRequestTarget(
      method: String,
      target: RequestTarget,
      version: String,
      headers: Headers
  ): Either[String, InitialRequestTarget] = {
    val hostValues = headers.getAll(HeaderNames.HOST)
    if (target.uriString.indexOf('#') >= 0) {
      Left("A request target must not contain a fragment")
    } else if (hostValues.sizeIs > 1) {
      Left("A request must not contain more than one Host header")
    } else if ("HTTP/1.1".equalsIgnoreCase(version) && hostValues.isEmpty) {
      Left("An HTTP/1.1 request must contain a Host header")
    } else {
      val hostAuthority = hostValues.headOption match {
        case Some(value) => parseAuthority(value).map(Some(_))
        case None        => Right(None)
      }

      hostAuthority.flatMap { parsedHost =>
        def requireNonEmptyHost[A](result: => Either[String, A]): Either[String, A] =
          Either
            .cond(
              parsedHost.forall(_.host.render.nonEmpty),
              (),
              "A Host header must contain a non-empty host"
            )
            .flatMap(_ => result)

        if (method == "CONNECT") {
          parseAuthority(target.uriString).flatMap { authority =>
            Either.cond(
              authority.host.render.nonEmpty && authority.port.flatMap(_.tcpPort).exists(_ > 0),
              InitialRequestTarget(None, Some(authority)),
              "A CONNECT request target must contain a non-empty host and a TCP port from 1 through 65535"
            )
          }
        } else if (target.uriString == "*") {
          requireNonEmptyHost {
            Either.cond(
              method == "OPTIONS",
              InitialRequestTarget(None, parsedHost),
              "Only an OPTIONS request may use the asterisk-form request target"
            )
          }
        } else if (target.uriString.startsWith("/")) {
          requireNonEmptyHost(Right(InitialRequestTarget(None, parsedHost)))
        } else {
          absoluteTarget(target.uriString) match {
            case Right(absolute) =>
              absolute.authority match {
                case Some(authorityValue) =>
                  for {
                    authority <- parseAuthority(authorityValue)
                    _         <- Either.cond(
                      authority.host.render.nonEmpty,
                      (),
                      "An absolute-form request target must contain a non-empty host"
                    )
                    result <- httpAbsoluteTarget(absolute.scheme, Some(authority))
                  } yield result
                case None =>
                  httpAbsoluteTarget(absolute.scheme, None)
              }
            case Left(error) => Left(error)
          }
        }
      }
    }
  }

  /**
   * Normalize the application-facing path of a validated request target while retaining its raw URI.
   *
   * RFC 3986 section 6.2.3 defines an empty path as equivalent to `/` for schemes with an
   * authority. [[InitialRequestTarget.scheme]] is present only for accepted HTTP(S) absolute-form
   * targets, so origin-form, asterisk-form, and CONNECT authority-form targets remain unchanged.
   */
  private[play] def normalizeRequestTargetPath(
      target: RequestTarget,
      initialTarget: InitialRequestTarget
  ): RequestTarget = {
    if (initialTarget.scheme.isDefined && target.path.isEmpty) target.withPath("/") else target
  }

  /** Derive authority for synthetic requests that do not enforce a wire protocol version. */
  private[play] def initialAuthority(
      method: String,
      target: RequestTarget,
      headers: Headers
  ): Either[String, Option[RequestAuthority]] =
    initialRequestTarget(method, target, "", headers).map(_.authority)

  /**
   * Reconcile an absolute target scheme with direct transport and trusted forwarding metadata.
   *
   * A trusted gateway may preserve the public absolute target or rewrite it to the scheme of its
   * backend connection. The final forwarded scheme contains only direct or proxy-trust-validated
   * metadata, so expose it as the canonical application-facing scheme while requiring the raw
   * target to describe one side of that trusted hop.
   */
  private[play] def effectiveScheme(
      targetScheme: Option[Scheme],
      directScheme: Scheme,
      forwardedScheme: Scheme
  ): Either[String, Scheme] = {
    targetScheme match {
      case Some(value) if value != directScheme && value != forwardedScheme =>
        Left(
          s"Absolute request scheme '${value.render}' matches neither direct scheme " +
            s"'${directScheme.render}' nor trusted forwarded scheme '${forwardedScheme.render}'"
        )
      case _ => Right(forwardedScheme)
    }
  }

  private[play] def absoluteTarget(target: String): Either[String, AbsoluteRequestTarget] = {
    UriScheme.findPrefixMatchOf(target) match {
      case Some(matched) =>
        for {
          scheme <- Scheme.parse(matched.group(1)).left.map(error => s"Invalid absolute request scheme: $error")
          // Play cannot route other schemes. Reject after recognizing the scheme instead of
          // parsing the remainder solely to refine an error for a request that will always fail.
          _ <- Either.cond(
            scheme == Scheme.Http || scheme == Scheme.Https,
            (),
            s"Unsupported absolute request scheme '${scheme.render}'"
          )
        } yield {
          val authority = Option.when(target.startsWith("//", matched.end)) {
            val start = matched.end + 2
            val end   = target.indexWhere(char => char == '/' || char == '?', start) match {
              case -1    => target.length
              case index => index
            }
            target.substring(start, end)
          }
          AbsoluteRequestTarget(scheme, authority)
        }
      case None =>
        Left("A request target must use origin-form, absolute-form, or OPTIONS asterisk-form")
    }
  }

  private def httpAbsoluteTarget(
      scheme: Scheme,
      authority: Option[RequestAuthority]
  ): Either[String, InitialRequestTarget] =
    Either.cond(
      authority.exists(_.host.render.nonEmpty),
      InitialRequestTarget(Some(scheme), authority),
      s"An absolute-form ${scheme.render} request target must contain a non-empty authority"
    )

  private def parseAuthority(value: String): Either[String, RequestAuthority] =
    RequestAuthority.parse(value).left.map(error => s"Invalid request authority: $error")

  /** Validate that generic header replacement does not mutate the effective authority. */
  private[mvc] def validateReplacementHeaders(
      headers: Headers,
      authority: Option[RequestAuthority]
  ): Headers = {
    val hostValues = headers.getAll(HeaderNames.HOST)
    if (hostValues.sizeIs > 1) {
      throw new IllegalArgumentException(
        "withHeaders cannot set duplicate Host headers; use withAuthority to replace the effective authority"
      )
    }

    (authority.map(_.render), hostValues.headOption) match {
      case (Some(expected), Some(actual)) if actual != expected =>
        throw new IllegalArgumentException(
          s"withHeaders cannot change Host from '$expected' to '$actual'; use withAuthority instead"
        )
      case (None, Some(actual)) =>
        throw new IllegalArgumentException(
          s"withHeaders cannot add Host '$actual' when the request has no authority; use withAuthority instead"
        )
      case _ => headers
    }
  }

  /** Expose the effective authority as the one canonical Host field. */
  private[mvc] def canonicalHeaders(headers: Headers, authority: Option[RequestAuthority]): Headers = {
    val underlying = headers match {
      case canonical: CanonicalAuthorityHeaders => canonical.underlying
      case other                                => other
    }
    val rendered = authority.map(_.render)
    val existing = underlying.getAll(HeaderNames.HOST)

    if (existing == rendered.toSeq) underlying
    else new CanonicalAuthorityHeaders(underlying, rendered)
  }

  private final class CanonicalAuthorityHeaders(
      val underlying: Headers,
      authority: Option[String]
  ) extends Headers(Seq.empty) {
    override def headers: Seq[(String, String)] =
      underlying.headers.filterNot(_._1.equalsIgnoreCase(HeaderNames.HOST)) ++
        authority.map(HeaderNames.HOST -> _)

    override def get(name: String): Option[String] = {
      if (name.equalsIgnoreCase(HeaderNames.HOST)) authority else underlying.get(name)
    }

    override def getAll(name: String): Seq[String] = {
      if (name.equalsIgnoreCase(HeaderNames.HOST)) authority.toSeq else underlying.getAll(name)
    }
  }

  // “The first "q" parameter (if any) separates the media-range parameter(s) from the accept-params.”
  val qPattern = ";\\s*q=([0-9.]+)".r

  /**
   * @return The items of an Accept* header, with their q-value.
   */
  private[play] def acceptHeader(headers: Headers, headerName: String): Seq[(Double, String)] = {
    for {
      header <- headers.get(headerName).toList
      value0 <- header.split(',')
      value = value0.trim
    } yield {
      RequestHeader.qPattern.findFirstMatchIn(value) match {
        case Some(m) => (m.group(1).toDouble, m.before.toString)
        case None    => (1.0, value) // “The default value is q=1.”
      }
    }
  }
}

/**
 * A standard implementation of a RequestHeader.
 */
private[play] class RequestHeaderImpl(
    override val remote: RemoteInfo,
    override val method: String,
    override val target: RequestTarget,
    override val version: String,
    requestHeaders: Headers,
    override val attrs: TypedMap,
    override val transport: TransportConnection,
    override val clientCertificate: Option[ClientCertificateInfo],
    override val scheme: Scheme,
    override val authority: Option[RequestAuthority],
    override val xForwardedClientCertificates: Vector[XForwardedClientCert] = Vector.empty
) extends RequestHeader {
  require(remote != null, "Selected remote metadata must not be null")
  require(transport != null, "Direct transport metadata must not be null")
  require(
    clientCertificate != null && clientCertificate.forall(_ != null),
    "Effective client certificate option must not be null or contain null"
  )
  require(scheme != null, "Effective request scheme must not be null")
  require(
    authority != null && authority.forall(_ != null),
    "Effective request authority option must not be null or contain null"
  )
  require(
    xForwardedClientCertificates != null && xForwardedClientCertificates.forall(_ != null),
    "The XFCC assertion sequence must not be null or contain null"
  )

  override val headers: Headers = RequestHeader.canonicalHeaders(requestHeaders, authority)
}
