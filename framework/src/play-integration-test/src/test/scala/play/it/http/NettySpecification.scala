/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http

import akka.util.Timeout
import com.typesafe.netty.http.pipelining.{ OrderedDownstreamChannelEvent, OrderedUpstreamMessageEvent }
import java.net.{ InetSocketAddress, SocketAddress }
import java.util.concurrent.{ BlockingQueue, LinkedBlockingQueue }
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import play.api.mvc._
import play.api.test._
import scala.concurrent.duration._

/**
 * A specification for testing Play's Netty integration.
 */
trait NettySpecification extends PlaySpecification with NettyRunners {

  /**
   * Test Netty handling against an EssentialAction using a QueueingChannel to
   * interact with the messages sent by PlayDefaultUpstreamHandler.
   */
  def withQueuingChannel[T](action: EssentialAction)(block: QueueingChannel => T): T = {
    val remoteAddress = new InetSocketAddress(8080)
    val downstreamHandler = new ChannelDownstreamHandler {
      def handleDownstream(ctx: ChannelHandlerContext, e: ChannelEvent) = {
        val received = receivedEvent(e)
        val qc = e.getChannel.asInstanceOf[QueueingChannel]
        qc.receivedQueue.add(received)
        qc.futureQueue.add(e.getFuture)
      }
      private def receivedEvent(e: ChannelEvent): Received = e match {
        case ode: OrderedDownstreamChannelEvent =>
          receivedEvent(ode.getChannelEvent)
        case me: MessageEvent =>
          Message(me.getMessage)
        case cse: ChannelStateEvent if (cse.getState == ChannelState.OPEN && cse.getValue == false) =>
          Close
        case _ =>
          Unknown(e)
      }
    }
    withDownstreamHandler(downstreamHandler, action) { pipeline =>
      val qc = new QueueingChannel(pipeline, remoteAddress)
      block(qc)
    }
  }

  /**
   * Simplified pattern-matchable events received by a HandlerInterface.
   * These are what tests operate on, rather than raw Netty events.
   */
  sealed trait Received
  case class Message(msg: Any) extends Received
  case object Close extends Received
  case class Unknown(e: ChannelEvent) extends Received

  /**
   * A special channel to allow testing of Play's message handling.
   *
   * Messages from upstream can be submitted with `sendOrderedMessage`.
   *
   * Events and messages sent downstream can be received with `receive`. Once
   * received, the result of downstream handling should be concluded
   * by calling either `succeed` or `fail`.
   *
   * The absence of a downstream message can be tested with `dontReceive`.
   */
  class QueueingChannel(pipeline: ChannelPipeline, remoteAddress: SocketAddress) extends StubChannel(pipeline, remoteAddress) {
    val futureQueue = new LinkedBlockingQueue[ChannelFuture]
    val receivedQueue = new LinkedBlockingQueue[Received]

    override def close(): ChannelFuture = {
      // Netty may call this method directly
      receivedQueue.add(Close)
      val closeFuture = getCloseFuture
      futureQueue.add(closeFuture)
      closeFuture
    }

    private def poll[T](q: BlockingQueue[T])(implicit pollTime: Timeout): Option[T] = {
      Option(q.poll(pollTime.duration.length, pollTime.duration.unit))
    }

    // Testing interface for channel

    /** Receive an event or message send downstream */
    def receive(implicit pollTime: Timeout): Received = {
      val opt = poll(receivedQueue)(pollTime)
      opt must beSome
      opt.get
    }

    /** Assert that no message or event has been sent downstream */
    def dontReceive(implicit pollTime: NegativeTimeout) = {
      val opt = poll(receivedQueue)(pollTime.t)
      opt must beNone
    }

    /** Indicate that the message or event sent downstream has succeeded */
    def succeed(implicit pollTime: Timeout) {
      poll(futureQueue)(pollTime).get.setSuccess()
    }

    /** Indicate that the message or event sent downstream has failed */
    def fail(t: Throwable)(implicit pollTime: Timeout) {
      poll(futureQueue)(pollTime).get.setFailure(t)
    }

    /** Send a message from upstream (to be handled by the Play handler) */
    def sendOrderedMessage(msg: AnyRef) = {
      val e = new OrderedUpstreamMessageEvent(orderedMessageCounter, this, msg, remoteAddress)
      orderedMessageCounter += 1
      pipeline.sendUpstream(e)
    }
    var orderedMessageCounter = 0 // Assume accessed from a single thread

  }
}