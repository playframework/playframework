/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs

/**
 * Asynchronous API to to query web services, as an http client.
 */
package object ws {
  @deprecated("Use WSRequest", "2.4.0")
  type WSRequestHolder = WSRequest
}
