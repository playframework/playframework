package views.js

import play.api.templates.JavaScript
import play.api.libs.json.{ Writes, Json }

/**
 * Contains helpers intended to be used in JavaScript templates
 */
package object helper {

  /**
   * Generates a JavaScript value from a Scala value.
   *
   * {{{
   *   @(username: String)
   *   alert(@helper.json(username));
   * }}}
   *
   * @param a The value to convert to JavaScript
   * @return A JavaScript value
   */
  def json[A: Writes](a: A): JavaScript = JavaScript(Json.toJson(a).toString)

}
