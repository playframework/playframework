/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc.request

import java.util.UUID

import scala.concurrent.Future

import org.apache.pekko.util.ByteString
import play.api.libs.streams.Accumulator
import play.api.libs.typedmap.TypedKey
import play.api.mvc.Cookies
import play.api.mvc.Flash
import play.api.mvc.Result
import play.api.mvc.Session

/**
 * Keys to request attributes.
 */
object RequestAttrKey {

  /**
   * The key for the request attribute storing a request id.
   */
  val Id = TypedKey[UUID]("Id")

  /**
   * The key for the request attribute storing a [[Cell]] with
   * [[play.api.mvc.Cookies]] in it.
   */
  val Cookies = TypedKey[Cell[Cookies]]("Cookies")

  /**
   * The key for the request attribute storing a [[Cell]] with
   * the [[play.api.mvc.Session]] cookie in it.
   */
  val Session = TypedKey[Cell[Session]]("Session")

  /**
   * The key for the request attribute storing a [[Cell]] with
   * the [[play.api.mvc.Flash]] cookie in it.
   */
  val Flash = TypedKey[Cell[Flash]]("Flash")

  /**
   * The key for the request attribute storing the server name.
   */
  val Server = TypedKey[String]("Server-Name")

  /**
   * The CSP nonce key.
   */
  val CSPNonce: TypedKey[String] = TypedKey("CSP-Nonce")

  val DeferredBodyParsing =
    TypedKey[(Future[Accumulator[ByteString, Result]], Boolean) => Future[Result]]("DeferredBodyParsing")
}
