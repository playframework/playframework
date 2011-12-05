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
  val defaultInput = defaultInputHandler.f

  /**
   * Default radio structure.
   *
   * {{{
   * <p>
   *   <input type="radio" name="country" id="country" value="germany">
   *   <label for="germany">Germany</label>
   * </p>
   * }}}
   */
  val defaultRadio = defaultRadioHandler.f

}