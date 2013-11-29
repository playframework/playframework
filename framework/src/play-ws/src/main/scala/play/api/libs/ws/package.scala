/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs

/**
 * Asynchronous API to to query web services, as an http client.
 */
package object ws {
  @deprecated("Use WSResponse", "2.3.0")
  type Response = WSResponse

  @deprecated("Use DefaultWSProxyServer", "2.3.0")
  type ProxyServer = DefaultWSProxyServer

  @deprecated("Use WSCookie", "2.3.0")
  type Cookie = WSCookie

  @deprecated("Use DefaultWSResponseHeaders", "2.3.0")
  type ResponseHeaders = DefaultWSResponseHeaders

  @deprecated("Use WSSignatureCalculator", "2.3.0")
  type SignatureCalculator = WSSignatureCalculator
}