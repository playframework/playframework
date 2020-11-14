/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed.oo

// #main
import javax.inject.Inject
import javax.inject.Singleton
import akka.actor.typed.ActorRef

@Singleton final class Main @Inject() (
    val helloActor: ActorRef[HelloActor.SayHello],
    val configuredActor: ActorRef[ConfiguredActor.GetConfig],
)
// #main
