/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.typed.fp

// #main
import javax.inject.Inject
import javax.inject.Singleton

import akka.actor.typed.ActorRef

@Singleton final class Main @Inject() (
    val helloActor: ActorRef[HelloActor.SayHello],
    val configuredActor: ActorRef[ConfiguredActor.GetConfig],
)
// #main
