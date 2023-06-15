/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.concurrent

import javax.inject.Singleton

import scala.reflect.ClassTag

import org.apache.pekko.actor.typed.scaladsl.adapter._
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.annotation.ApiMayChange
import com.google.inject.Inject
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Provider
import play.api.libs.concurrent.TypedAkka._

/**
 * A singleton [[Provider]] of the typed `ActorRef[T]` resulting from spawning an actor with the
 * `Behavior[T]` in dependency scope and the given name, in the [[ActorSystem]] in dependency scope.
 *
 * @param name the name to use when spawning the typed actor.
 * @tparam T The class of the messages the typed actor can handle.
 */
@Singleton
@ApiMayChange
final class TypedActorRefProvider[T: ClassTag](val name: String) extends Provider[ActorRef[T]] {
  @Inject private val actorSystem: ActorSystem = null
  @Inject private val guiceInjector: Injector  = null

  lazy val get = {
    val behavior = guiceInjector.getInstance(Key.get(behaviorOf[T]))
    actorSystem.spawn(behavior, name)
  }
}
