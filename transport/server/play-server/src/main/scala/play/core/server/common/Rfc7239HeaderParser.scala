/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.common

import java.util.Locale

import scala.collection.mutable

/** Parses RFC 7239 field syntax. Parameter-specific values are parsed later. */
private[common] object Rfc7239HeaderParser {

  def parse(input: String): Either[String, Vector[Map[String, String]]] = new Parser(input).parse()

  private final class Parser(input: String) {
    private var index = 0

    def parse(): Either[String, Vector[Map[String, String]]] = {
      val elements = Vector.newBuilder[Map[String, String]]

      skipOptionalWhitespace()
      while (index < input.length) {
        if (input.charAt(index) == ',') {
          // HTTP list recipients ignore empty list elements.
          index += 1
          skipOptionalWhitespace()
        } else {
          parseElement() match {
            case Left(error)    => return Left(error)
            case Right(element) =>
              elements += element
          }

          skipOptionalWhitespace()
          if (index < input.length) {
            if (input.charAt(index) != ',') {
              return Left(s"Expected ',' at position $index")
            }
            index += 1
            skipOptionalWhitespace()
          }
        }
      }

      Right(elements.result())
    }

    private def parseElement(): Either[String, Map[String, String]] = {
      val parameters = mutable.LinkedHashMap.empty[String, String]

      if (input.charAt(index) != ';') {
        parsePair(parameters) match {
          case Left(error) => return Left(error)
          case Right(_)    =>
        }
      }

      while (index < input.length && input.charAt(index) == ';') {
        index += 1
        // Play 3.0 partially accepted fields containing whitespace after `;`:
        // it retained the preceding parameter but lost the whitespace-prefixed
        // one. Accept that whitespace as a bounded compatibility allowance so
        // the complete field is retained, while keeping all other RFC 7239
        // parameter whitespace invalid.
        skipOptionalWhitespace()
        if (index < input.length && input.charAt(index) != ';' && input.charAt(index) != ',') {
          parsePair(parameters) match {
            case Left(error) => return Left(error)
            case Right(_)    =>
          }
        }
      }

      Right(parameters.toMap)
    }

    private def parsePair(parameters: mutable.Map[String, String]): Either[String, Unit] = {
      val rawName = parseToken() match {
        case Left(error) => return Left(error)
        case Right(name) => name
      }
      val name = rawName.toLowerCase(Locale.ENGLISH)

      if (index >= input.length || input.charAt(index) != '=') {
        return Left(s"Expected '=' after parameter '$rawName' at position $index")
      }
      index += 1

      val value =
        if (index < input.length && input.charAt(index) == '"') parseQuotedString()
        else if (name == "for" || name == "by") parseLegacyNodeValue()
        else parseToken()

      value.flatMap { parsedValue =>
        if (parameters.contains(name)) {
          Left(s"Duplicate parameter '$rawName'")
        } else {
          parameters += name -> parsedValue
          Right(())
        }
      }
    }

    private def parseToken(): Either[String, String] = {
      val start = index
      while (index < input.length && isTokenCharacter(input.charAt(index))) {
        index += 1
      }
      if (index == start) Left(s"Expected token at position $index")
      else Right(input.substring(start, index))
    }

    /**
     * Play 3.0 accepted unquoted RFC 7239 `for` values containing IPv6 brackets
     * or ports. Preserve that behavior and apply the same allowance to `by` so
     * both node parameters are parsed consistently. Other parameter values remain
     * restricted to the RFC token grammar.
     */
    private def parseLegacyNodeValue(): Either[String, String] = {
      val start = index
      while (
        index < input.length &&
        (isTokenCharacter(input.charAt(index)) || ":[]".indexOf(input.charAt(index)) >= 0)
      ) {
        index += 1
      }
      if (index == start) Left(s"Expected node value at position $index")
      else Right(input.substring(start, index))
    }

    private def parseQuotedString(): Either[String, String] = {
      val value = new StringBuilder
      index += 1

      while (index < input.length) {
        val char = input.charAt(index)
        if (char == '"') {
          index += 1
          return Right(value.result())
        } else if (char == '\\') {
          index += 1
          if (index >= input.length || !isQuotedPairCharacter(input.charAt(index))) {
            return Left(s"Invalid quoted-pair at position $index")
          }
          value.append(input.charAt(index))
          index += 1
        } else if (isQuotedTextCharacter(char)) {
          value.append(char)
          index += 1
        } else {
          return Left(s"Invalid quoted-string character at position $index")
        }
      }

      Left("Unterminated quoted-string")
    }

    private def skipOptionalWhitespace(): Unit = {
      while (index < input.length && isOptionalWhitespace(input.charAt(index))) {
        index += 1
      }
    }

    private def isOptionalWhitespace(char: Char): Boolean = char == ' ' || char == '\t'

    private def isTokenCharacter(char: Char): Boolean = {
      (char >= 'a' && char <= 'z') || (char >= 'A' && char <= 'Z') ||
      (char >= '0' && char <= '9') || "!#$%&'*+-.^_`|~".indexOf(char) >= 0
    }

    private def isQuotedTextCharacter(char: Char): Boolean = {
      char == '\t' || char == ' ' || char == '!' ||
      (char >= '#' && char <= '[') || (char >= ']' && char <= '~') ||
      (char >= '\u0080' && char <= '\u00ff')
    }

    private def isQuotedPairCharacter(char: Char): Boolean = {
      char == '\t' || char == ' ' || (char >= '!' && char <= '~') ||
      (char >= '\u0080' && char <= '\u00ff')
    }
  }
}
