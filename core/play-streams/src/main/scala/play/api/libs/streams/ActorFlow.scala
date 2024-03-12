/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.streams

import org.apache.pekko.actor._
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.Keep
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.OverflowStrategy

/**
 * Provides a flow that is handled by an actor.
 *
 * See https://github.com/akka/akka/issues/16985.
 */
object ActorFlow {

  /**
   * Create a flow that is handled by an actor.
   *
   * Messages can be sent downstream by sending them to the actor passed into the props function.  This actor meets
   * the contract of the actor returned by [[https://pekko.apache.org/api/pekko/1.0/org/apache/pekko/stream/scaladsl/Source$.html#actorRef[T](bufferSize:Int,overflowStrategy:org.apache.pekko.stream.OverflowStrategy):org.apache.pekko.stream.scaladsl.Source[T,org.apache.pekko.actor.ActorRef]] org.apache.pekko.stream.scaladsl.Source.actorRef]].
   *
   * The props function should return the props for an actor to handle the flow. This actor will be created using the
   * passed in [[https://pekko.apache.org/api/pekko/1.0/org/apache/pekko/actor/ActorRefFactory.html org.apache.pekko.actor.ActorRefFactory]]. Each message received will be sent to the actor - there is no back pressure,
   * if the actor is unable to process the messages, they will queue up in the actors mailbox. The upstream can be
   * cancelled by the actor terminating itself.
   *
   * @param props A function that creates the props for actor to handle the flow.
   * @param bufferSize The maximum number of elements to buffer.
   * @param overflowStrategy The strategy for how to handle a buffer overflow.
   */
  def actorRef[In, Out](
      props: ActorRef => Props,
      bufferSize: Int = 16,
      overflowStrategy: OverflowStrategy = OverflowStrategy.fail
  )(implicit factory: ActorRefFactory, mat: Materializer): Flow[In, Out, ?] = {
    val (outActor, publisher) = Source
      .actorRef[Out](bufferSize, overflowStrategy)
      .toMat(Sink.asPublisher(false))(Keep.both)
      .run()

    Flow.fromSinkAndSource(
      Sink.actorRef(
        factory.actorOf(Props(new Actor {
          val flowActor = context.watch(context.actorOf(props(outActor), "flowActor"))

          def receive = {
            case _: Status.Success => flowActor ! PoisonPill
            case _: Status.Failure => flowActor ! PoisonPill
            case _: Terminated     => context.stop(self)
            case other             => flowActor ! other
          }

          override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
            case _ => SupervisorStrategy.Stop
          }
        })),
        Status.Success(())
      ),
      Source.fromPublisher(publisher)
    )
  }
}
