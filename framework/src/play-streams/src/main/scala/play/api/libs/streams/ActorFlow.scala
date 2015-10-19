package play.api.libs.streams

import akka.actor._
import akka.stream.{ Materializer, OverflowStrategy }
import akka.stream.scaladsl.{ Sink, Keep, Source, Flow }

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
   * the contract of the actor returned by [[Source.actorRef()]].
   *
   * The props function should return the props for an actor to handle the flow. This actor will be created using the
   * passed in [[ActorRefFactory]]. Each message received will be sent to the actor - there is no back pressure,
   * if the actor is unable to process the messages, they will queue up in the actors mailbox. The upstream can be
   * cancelled by the actor terminating itself.
   *
   * @param props A function that creates the props for actor to handle the flow.
   * @param bufferSize The maximum number of elements to buffer.
   * @param overflowStrategy The strategy for how to handle a buffer overflow.
   */
  def actorRef[In, Out](props: ActorRef => Props, bufferSize: Int = 16, overflowStrategy: OverflowStrategy = OverflowStrategy.dropNew)(implicit factory: ActorRefFactory, mat: Materializer): Flow[In, Out, Unit] = {

    val (outActor, publisher) = Source.actorRef[Out](bufferSize, overflowStrategy)
      .toMat(Sink.publisher)(Keep.both).run()

    Flow.wrap(
      Sink.actorRef(factory.actorOf(Props(new Actor {
        val flowActor = context.watch(context.actorOf(props(outActor), "flowActor"))

        def receive = {
          case Status.Success(_) | Status.Failure(_) => flowActor ! PoisonPill
          case Terminated =>
            println("Child terminated, stopping")
            context.stop(self)
          case other => flowActor ! other
        }

        override def supervisorStrategy = OneForOneStrategy() {
          case _ =>
            println("Stopping actor due to exception")
            SupervisorStrategy.Stop
        }
      })), Status.Success(())),
      Source(publisher)
    )(Keep.none)
  }
}
