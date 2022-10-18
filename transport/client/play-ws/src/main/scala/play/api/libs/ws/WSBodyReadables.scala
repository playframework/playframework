/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.ws

/**
 * JSON, XML and Multipart Form Data Readables used for Play-WS bodies.
 */
trait WSBodyReadables extends DefaultBodyReadables with JsonBodyReadables with XMLBodyReadables

object WSBodyReadables extends WSBodyReadables
