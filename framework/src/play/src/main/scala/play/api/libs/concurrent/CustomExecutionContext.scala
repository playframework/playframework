/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import akka.actor.ActorSystem
import akka.dispatch.MessageDispatcher

import scala.concurrent.ExecutionContextExecutor

/**
 * This class defines a custom execution context that delegates to an akka.actor.ActorSystem.
 *
 * It is very useful for situations in which the default execution context should not
 * be used, for example if a database or blocking I/O is being used.
 *
 * To define a custom context, subclass CustomExecutionContext with the dispatcher
 * name:
 *
 * {{{
 * @Singleton
 * class DatabaseExecutionContext @Inject()(system: ActorSystem)
 *    extends CustomExecutionContext(system, "database-dispatcher")
 * }}}
 *
 * and then bind it in dependency injection:
 *
 * {{{
 * bind[DatabaseExecutionContext].to(classOf[DatabaseExecutionContext]).asEagerSingleton()
 * }}}
 *
 * Then have the execution context passed in as an implicit parameter:
 *
 * {{{
 * class DatabaseService @Inject()(implicit executionContext: DatabaseExecutionContext) {
 *   ...
 * }
 * }}}
 *
 * @see <a href="http://doc.akka.io/docs/akka/2.5/scala/dispatchers.html">Dispatchers</a>
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
