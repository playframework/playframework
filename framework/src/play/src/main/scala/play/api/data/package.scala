package play.api

import data.format._
import data.validation._

/**
 * Contains data manipulation helpers (typically HTTP form handling)
 *
 * {{{
 * import play.api.data._
 * import play.api.data.Forms._
 *
 * val taskForm = Form(
 *   of(Task.apply _, Task.unapply _)(
 *     "name" -> text(minLength = 3),
 *     "dueDate" -> date("yyyy-MM-dd"),
 *     "done" -> boolean
 *   )
 * )
 * }}}
 *
 */
package object data {

}
