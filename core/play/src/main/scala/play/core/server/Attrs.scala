/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server

import akka.util.ByteString
import play.api.libs.streams.Accumulator
import play.api.libs.typedmap.TypedKey
import play.api.mvc.Result

import scala.concurrent.Future

object Attrs {
  private[play] val DeferredBodyParserInvoker
      : TypedKey[(Future[Accumulator[ByteString, Result]], Boolean) => Future[Result]] =
    TypedKey.apply("DeferredBodyParserInvoker")
}
