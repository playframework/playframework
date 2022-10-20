/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

/**
 * This exception occurs when the Play server receives a request header
 * where at least one character is illegal according to RFC2616 and RFC7230
 *
 * @param message The reason for the exception.
 * @param character The invalid character.
 */
class InvalidHeaderCharacterException(message: String, val character: Char) extends Exception(message)
