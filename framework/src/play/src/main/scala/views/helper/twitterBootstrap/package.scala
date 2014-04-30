/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package views.html.helper

/**
 * Contains template helpers, for example for generating HTML forms.
 */
package object twitterBootstrap {

  /**
   * Twitter bootstrap input structure.
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
  @deprecated("The twitter bootstrap field constructor will be removed from Play in 2.4 since the way Bootstrap must be used changes too frequently and too drastically between versions for this to make sense to be in the core of Play", "2.3")
  implicit val twitterBootstrapField = new FieldConstructor {
    def apply(elts: FieldElements) = twitterBootstrapFieldConstructor(elts)
  }

}