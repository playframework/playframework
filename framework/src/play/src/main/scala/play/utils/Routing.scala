package play.utils

import java.util.regex.Pattern

object Routing {
  def prepareString(string: String, captureFirstRegex: Boolean = false): String = {
    // Quote plain (non-regex) pieces. Leave regexes as is, but if wrapFirstRegex is true, wrap the first
    // regex in parentheses beause it's part of a route matcher.
    val regexStart = string.indexOf("<")
    val regexEnd = string.indexOf(">")
    if (regexStart > -1 && regexEnd > regexStart) {
      val regexBody = string.slice(regexStart + 1, regexEnd)
      val regex = if (captureFirstRegex) "(" + regexBody + ")" else regexBody
      prepareString(string.take(regexStart)) + regex + prepareString(string.drop(regexEnd + 1))
    } else {
      if (string.isEmpty) "" else Pattern.quote(string)
    }
  }
}