package views.html.helper

/**
 * Contains template helpers, for example for generating HTML forms.
 */
package object twitterBootstrap20 {

  /**
   * Twitter bootstrap input structure.
   *
   * {{{
   * <div class="control-group" id="username_field">
   *   <label for="username" class="control-label">User name</label>
   *   <div class="controls">
   *     <input type="text" id="username" name="username" value="" >
   *     <span class="help-block">Required</span>
   *   </div>
   * </div>
   * }}}
   */
  implicit val twitterBootstrap20Field = new FieldConstructor {
    def apply(elts: FieldElements) = twitterBootstrap20FieldConstructor(elts)
  }

}