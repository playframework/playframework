/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.actors

import akka.actor._
import play.api.libs.iteratee._
import akka.actor.Terminated
import scala.reflect.ClassTag

/**
 * Integration between Play WebSockets and actors
 */
private[play] object WebSocketActor {

  object WebSocketActorSupervisor {
    def props[In, Out: ClassTag](enumerator: Enumerator[In], iteratee: Iteratee[Out, Unit],
      createHandler: ActorRef => Props) =
      Props(new WebSocketActorSupervisor[In, Out](enumerator, iteratee, createHandler))
  }

  /**
   * The actor that supervises and handles all messages to/from the WebSocket actor.
   */
  private class WebSocketActorSupervisor[In, Out](enumerator: Enumerator[In], iteratee: Iteratee[Out, Unit],
      createHandler: ActorRef => Props)(implicit messageType: ClassTag[Out]) extends Actor {

    import context.dispatcher

    /*
     * There are two ways that this actor might shutdown.  One is that the child might send a poison pill.  The other
     * is that the WebSocket might close, which will send a PoisonPill.  The problem is, if the child sends a poison
     * pill, then the WebSocket will close as a result, but there's no way to cancel the callback being invoked when
     * the WebSocket closes.  So that callback will send a PoisonPill, but that PoisonPill will end up in the dead
     * letter queue because the actor will already be stopped.  So, to prevent that second PoisonPill from being sent,
     * we use this flag.
     *
     * There still is of course a race condition - the client might close the WebSocket at the same time as the child
     * actor decides to close, and then two PoisonPills might still be sent - we can't avoid that.  But this just
     * prevents the double PoisonPill from being a normal thing that happens every time a child initiates shutdown.
     */
    @volatile var shutdown = false

    // The actor to handle the WebSocket
    val webSocketActor = context.watch(context.actorOf(createHandler(self), "handler"))

    // Use a broadcast enumerator to imperatively push messages into the WebSocket
    val channel = {
      val (enum, chan) = Concurrent.broadcast[Out]
      // Ensure we feed EOF into the iteratee when done, to ensure that the WebSocket gets closed
      enum |>>> iteratee
      chan
    }

    // Use a foreach iteratee to consume the WebSocket and feed it into the Actor
    // It's very important that we use the trampoline execution context here, otherwise it's possible that
    val consumer = Iteratee.foreach[In] { msg =>
      webSocketActor ! msg
    }(play.api.libs.iteratee.Execution.trampoline)

    (enumerator |>> consumer).onComplete { _ =>
      // When the WebSocket is complete, either due to an error or not, shutdown
      if (!shutdown)
        webSocketActor ! PoisonPill
    }

    def receive = {
      case _: Terminated =>
        shutdown = true
        // Child has terminated, close the WebSocket.
        channel.end()
        context.stop(self)
      // A message of the type that we're handling has been received
      case messageType(a) => channel.push(a)
    }

    override def postStop() = {
      shutdown = true
      // In the normal shutdown case, this will already have been called, that's ok, channel.end() is a no-op in that
      // case.  This does however handle the case where this supervisor crashes, or when it's stopped externally.
      channel.end()
    }

    override def supervisorStrategy = OneForOneStrategy() {
      case _ => SupervisorStrategy.Stop
    }
  }

  object WebSocketsActor {

    val props = Props(new WebSocketsActor)

    /**
     * Connect an actor to the WebSocket on the end of the given enumerator/iteratee.
     *
     * @param requestId The requestId. Used to name the actor.
     * @param enumerator The enumerator to send messages to.
     * @param iteratee The iteratee to consume messages from.
     * @param createHandler A function that creates a handler to handle the WebSocket, given an actor to send messages
     *                      to.
     * @param messageType The type of message this WebSocket deals with.
     */
    case class Connect[In, Out](requestId: Long, enumerator: Enumerator[In], iteratee: Iteratee[Out, Unit],
      createHandler: ActorRef => Props)(implicit val messageType: ClassTag[Out])
  }

  /**
   * The actor responsible for creating all web sockets
   */
  private class WebSocketsActor extends Actor {
    import WebSocketsActor._

    def receive = {
      case c @ Connect(requestId, enumerator, iteratee, createHandler) =>
        implicit val mt = c.messageType
        context.actorOf(WebSocketActorSupervisor.props(enumerator, iteratee, createHandler),
          requestId.toString)
    }
  }

  /**
   * The extension for managing WebSockets
   */
  object WebSocketsExtension extends ExtensionId[WebSocketsExtension] {
    def createExtension(system: ExtendedActorSystem) = {
      new WebSocketsExtension(system.systemActorOf(WebSocketsActor.props, "websockets"))
    }
  }

  class WebSocketsExtension(val actor: ActorRef) extends Extension
}
