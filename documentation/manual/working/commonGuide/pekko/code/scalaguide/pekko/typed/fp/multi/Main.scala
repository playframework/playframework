/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.pekko.typed.fp
package multi

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.apache.pekko.actor.typed.ActorRef

@Singleton final class Main @Inject() (
    @Named("hello-actor1") val helloActor1: ActorRef[HelloActor.SayHello],
    @Named("hello-actor2") val helloActor2: ActorRef[HelloActor.SayHello],
    @Named("configured-actor1") val configuredActor1: ActorRef[ConfiguredActor.GetConfig],
    @Named("configured-actor2") val configuredActor2: ActorRef[ConfiguredActor.GetConfig],
)
