/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.concurrent

import akka.actor.ActorSystem
import akka.dispatch.MessageDispatcher

import scala.concurrent.ExecutionContextExecutor

/**
 * This class defines a custom execution context that delegates to an [[ActorSystem]].
 *
 * It is very useful for situations in which the default execution context should not
 * be used, for example if a database or blocking I/O is being used.
 *
 * To define a custom context, subclass DispatcherExecutionContext with the dispatcher
 * name and have it bound through dependency injection:
 *
 * {{{
 * @Singleton
 * class DatabaseExecutionContext @Inject()(system: ActorSystem)
 *    extends CustomExecutionContext(system, "database-dispatcher")
 * }}}
 *
 * and then have the execution context passed in as a class parameter:
 *
 * {{{
 * class DatabaseService @Inject()(implicit executionContext: DatabaseExecutionContext) {
 *   ...
 * }
 * }}}
 *
 * @see <a href="http://doc.akka.io/docs/akka/current/java/dispatchers.html">Dispatchers</a>
 * @see <a href="https://www.playframework.com/documentation/latest/ThreadPools">Thread Pools</a>
 *
 * @param system the actor system
 * @param name   the full path of the dispatcher name in Typesafe Config.
 */
abstract class CustomExecutionContext(system: ActorSystem, name: String) extends ExecutionContextExecutor {
  private val dispatcher: MessageDispatcher = system.dispatchers.lookup(name)

  override def execute(command: Runnable) = dispatcher.execute(command)

  override def reportFailure(cause: Throwable) = dispatcher.reportFailure(cause)
}
