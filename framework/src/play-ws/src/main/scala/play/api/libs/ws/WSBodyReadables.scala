/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.ws

/**
 * A set of useful [[BodyReadable]] implicit classes.
 */
trait WSBodyReadables extends DefaultBodyReadables with JsonBodyReadables with XMLBodyReadables

object WSBodyReadables extends WSBodyReadables
