/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.ws

/**
 * JSON, XML and Multipart Form Data Readables used for Play-WS bodies.
 */
trait WSBodyReadables extends DefaultBodyReadables with JsonBodyReadables with XMLBodyReadables

object WSBodyReadables extends WSBodyReadables
