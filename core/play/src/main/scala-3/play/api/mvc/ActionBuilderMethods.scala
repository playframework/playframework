/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import scala.concurrent.Future

trait ActionBuilderMethods[+R[_], B] { self: ActionBuilder[R, B] =>

  /**
   * Constructs an `Action` that returns a future of a result, with default content.
   *
   * @param block the action code
   * @return an action
   */
  final def handle(block: R[B] ?=> Future[Result]): Action[B] =
    async(request => block(using request))

  /**
   * Constructs an `Action` with the given [[BodyParser]] that returns a future of a result.
   *
   * @param block the action code
   * @return an action
   */
  final def handle[A](bodyParser: BodyParser[A])(block: R[A] ?=> Future[Result]): Action[A] =
    async(bodyParser)(request => block(using request))
}
