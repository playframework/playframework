/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import akka.annotation.ApiMayChange
import com.google.inject.AbstractModule

/**
 * Allows runtime dependency injection of actor behavior, defined in "functional programming" style.
 *
 * 1. Mix this trait into the `object` defining the actor messages and the actor behavior(s),
 * 2. Define the `Message` type with actor message class,
 * 3. Add the [[com.google.inject.Provides Provides]] annotation to the method that returns the
 *   (initial) [[akka.actor.typed.Behavior Behavior]] of the actor.
 * 4. Use the `bindTypedActor` in [[AkkaGuiceSupport]], passing the `object` as the actor module.
 *
 * For example:
 * {{{
 *   object ConfiguredActor extends ActorModule {
 *     type Message = GetConfig
 *
 *     final case class GetConfig(replyTo: ActorRef[String])
 *
 *     @Provides def apply(configuration: Configuration): Behavior[GetConfig] = {
 *       // TODO: Define ConfiguredActor's behavior using the injected configuration.
 *       Behaviors.empty
 *     }
 *   }
 *
 *   final class AppModule extends AbstractModule with AkkaGuiceSupport {
 *     override def configure() = {
 *       bindTypedActor(classOf[ConfiguredActor], "configured-actor")
 *     }
 *   }
 * }}}
 *
 * @see https://doc.akka.io/docs/akka/snapshot/typed/style-guide.html#functional-versus-object-oriented-style
 */
@ApiMayChange
trait ActorModule extends AbstractModule {
  type Message
}

/** The companion object to hold [[ActorModule]]'s [[ActorModule.Aux]] type alias. */
@ApiMayChange
object ActorModule {

  /** A convenience to refer to the type of an [[ActorModule]] with the given message type [[A]]. */
  type Aux[A] = ActorModule { type Message = A }

}
