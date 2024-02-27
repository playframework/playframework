/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import com.google.inject.AbstractModule
import org.apache.pekko.annotation.ApiMayChange

/**
 * Facilitates runtime dependency injection of "functional programming"-style actor behaviors.
 *
 * 1. Mix this trait into the `object` defining the actor message(s) and behavior(s);
 * 2. Define the `Message` type with actor message class;
 * 3. Annotate with [[com.google.inject.Provides Provides]] the "create" method that returns the
 *   (possibly just initial) [[org.apache.pekko.actor.typed.Behavior Behavior]] of the actor;
 * 4. Use the `bindTypedActor` in [[PekkoGuiceSupport]], passing the `object` as the actor module.
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
 *   final class AppModule extends AbstractModule with PekkoGuiceSupport {
 *     override def configure() = {
 *       bindTypedActor(ConfiguredActor, "configured-actor")
 *     }
 *   }
 * }}}
 *
 * <p>`Message` is a type member rather than a type parameter is because you can't define, using the
 * example above, `GetConfig` inside the object and also have the object extend
 * `ActorModule[ConfiguredActor.GetConfig]`.
 *
 * @see https://pekko.apache.org/docs/pekko/1.0/typed/style-guide.html#functional-versus-object-oriented-style
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
