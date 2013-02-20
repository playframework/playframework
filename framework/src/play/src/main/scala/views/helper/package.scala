package views.html

import play.api.libs.json.{Json, Writes}
import play.api.templates.Html

/**
 * Contains template helpers, for example for generating HTML forms.
 */
package object helper {

  /**
   * Default inpout structure.
   *
   * {{{
   * <dl>
   *   <dt><label for="username"></dt>
   *   <dd><input type="text" name="username" id="username"></dd>
   *   <dd class="error">This field is required!</dd>
   *   <dd class="info">Required field.</dd>
   * </dl>
   * }}}
   */
  val defaultField = defaultFieldConstructor.f

  /**
   * @return The url-encoded value of `string` using the charset provided by `codec`
   */
  def urlEncode(string: String)(implicit codec: play.api.mvc.Codec): String =
    java.net.URLEncoder.encode(string, codec.charset)

  /**
   * Generates a JavaScript value from a Scala value. This is useful when you need to generate JavaScript
   * values in your templates:
   *
   * {{{
   * @(username: String)
   * <script>
   *   alert(@helper.json(username));
   * </script>
   * }}}
   *
   * @param a The value to convert to JavaScript
   * @return A JavaScript value
   */
  def json[A : Writes](a: A): Html = Html(Json.toJson(a).toString)
}