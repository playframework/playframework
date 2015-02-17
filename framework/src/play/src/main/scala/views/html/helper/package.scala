/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package views.html

/**
 * Contains template helpers, for example for generating HTML forms.
 */
package object helper {

  /**
   * Default input structure.
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

}
