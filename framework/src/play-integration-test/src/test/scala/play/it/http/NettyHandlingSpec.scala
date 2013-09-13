package play.it.http

import akka.util.Timeout
import com.typesafe.netty.http.pipelining.{ OrderedDownstreamChannelEvent, OrderedUpstreamMessageEvent }
import java.net.{ InetSocketAddress, SocketAddress }
import java.util.concurrent.{ BlockingQueue, LinkedBlockingQueue }
import org.jboss.netty.channel._
import org.jboss.netty.channel.group.DefaultChannelGroup
import org.jboss.netty.handler.codec.http._
import org.specs2.mutable.Specification
import play.api.{ Play, Application, Mode }
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import play.core.{ ApplicationProvider }
import play.core.Router.Routes
import play.core.server.Server
import play.core.server.netty.PlayDefaultUpstreamHandler
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

/**
 * Tests for Play's interactions with Netty. Allows observation and control of
 * individual Netty events, e.g. can simulate failure sending a response and
 * observe whether the connection is closed properly.
 */
object NettyHandlingSpec extends PlaySpecification {

  /**
   * How long we should wait for something that we expect *not* to happen, e.g.
   * waiting to make sure that a channel is *not* closed by some concurrent process.
   *
   * NegativeTimeout has a separate type to a normal Timeout because it will often
   * have a lower value. This is because there is no way to prove that nothing has
   * happened in a particular time except by waiting for that full time, so setting
   * a high value will result in a lot of time spent waiting, and tests will run
   * slowly.
   *
   * Setting the value too low runs the risk of missing out on an event that occurs
   * just after that negative timeout elapses, but this is sometimes an acceptable
   * risk.
   */
  case class NegativeTimeout(t: Timeout)
  implicit val negativeTimeout = NegativeTimeout(200.millis)

  /**
   * Test Netty handling in a block of code for an application with a single EssentialAction.
   */
  def testHandling[T](action: EssentialAction)(block: HandlerInterface => T) = {
    val app = new FakeApplication() {
      override lazy val routes = Some(new Routes {
        def prefix = "/"
        def setPrefix(prefix: String) {}
        def documentation = Nil
        def routes = {
          case _ => action
        }
      })
    }
    running(app) {
      // Create a minimal Server needed by PlayDefaultUpstreamHandler
      val appProvider = new ApplicationProvider {
        def get = Success(app)
        def path = app.path
      }
      val server = new Server {
        def mode: Mode.Mode = Mode.Test
        def applicationProvider: ApplicationProvider = appProvider
      }
      // Create a PlayDefaultUpstreamHandler and an interface to interact with it
      val duh = new PlayDefaultUpstreamHandler(server, new DefaultChannelGroup())
      val remoteAddress = new InetSocketAddress(8080)
      val hi = new HandlerInterface(remoteAddress, duh)
      try block(hi) finally Play.stop()
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
   * An interface to Play's Netty handling.
   *
   * Messages from upstream can be submitted with `sendOrderedMessage`.
   *
   * Events and messages sent downstream can be received with `receive`. Once
   * received, the result of downstream handling should be concluded
   * by calling either `succeed` or `fail`.
   *
   * The absence of a downstream message can be tested with `dontReceive`.
   */
  class HandlerInterface(remoteAddress: SocketAddress, duh: PlayDefaultUpstreamHandler) {

    private val futureQueue = new LinkedBlockingQueue[ChannelFuture]
    private val receivedQueue = new LinkedBlockingQueue[Received]

    private def poll[T](q: BlockingQueue[T])(implicit pollTime: Timeout): Option[T] = {
      Option(q.poll(pollTime.duration.length, pollTime.duration.unit))
    }

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

    // A downstream handler that queues all its messages and futures
    private val qdh = new ChannelDownstreamHandler {
      def handleDownstream(ctx: ChannelHandlerContext, e: ChannelEvent) = {
        receivedQueue.add(receivedEvent(e))
        futureQueue.add(e.getFuture)
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
    private val pipeline = Channels.pipeline()
    pipeline.addFirst("play-upstream-handler", duh)
    pipeline.addFirst("queueing-downstream-handler", qdh)

    /** Send a message from upstream (to be handled by the Play handler) */
    def sendOrderedMessage(msg: AnyRef) = {
      val e = new OrderedUpstreamMessageEvent(orderedMessageCounter, channel, msg, remoteAddress)
      orderedMessageCounter += 1
      pipeline.sendUpstream(e)
    }
    var orderedMessageCounter = 0 // Assume accessed from a single thread
    // A minimal Channel
    val channel = new Channel {
      val closeFuture = new DefaultChannelFuture(this, false)

      def getId: Integer = ???
      def getFactory: ChannelFactory = ???
      def getParent: Channel = ???
      def getConfig: ChannelConfig = ???
      def getPipeline: ChannelPipeline = pipeline
      def isOpen: Boolean = ???
      def isBound: Boolean = ???
      def isConnected: Boolean = ???
      def getLocalAddress: SocketAddress = ???
      def getRemoteAddress: SocketAddress = remoteAddress
      def write(message: Any): ChannelFuture = ???
      def write(message: Any, remoteAddress: SocketAddress): ChannelFuture = ???
      def bind(localAddress: SocketAddress): ChannelFuture = ???
      def connect(remoteAddress: SocketAddress): ChannelFuture = ???
      def disconnect(): ChannelFuture = ???
      def unbind(): ChannelFuture = ???
      def close(): ChannelFuture = {
        // Netty may call this method directly
        receivedQueue.add(Close)
        futureQueue.add(closeFuture)
        closeFuture
      }
      def getCloseFuture: ChannelFuture = closeFuture
      def getInterestOps: Int = ???
      def isReadable: Boolean = ???
      def isWritable: Boolean = ???
      def setInterestOps(interestOps: Int): ChannelFuture = ???
      def setReadable(readable: Boolean): ChannelFuture = ???
      def getAttachment: AnyRef = ???
      def setAttachment(attachment: Any) { ??? }
      def compareTo(o: Channel): Int = ???
    }
  }

  "Play's Netty handling" should {

    "handle HTTP/1.1 requests without Connection headers" in testHandling(Action(Results.Ok)) { hi =>
      val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
      hi.sendOrderedMessage(request)

      val responseMessage = hi.receive.asInstanceOf[Message]
      val httpResponse = responseMessage.msg.asInstanceOf[DefaultHttpResponse]
      httpResponse.getStatus.getCode must_== OK
      httpResponse.getHeader(HttpHeaders.Names.CONTENT_LENGTH) must_== "0"
      hi.succeed

      hi.dontReceive // Must not close connection
    }

    "handle HTTP/1.1 requests with 'Connection: Keep-Alive' headers" in testHandling(Action(Results.Ok)) { hi =>
      val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
      request.addHeader(CONNECTION, "Keep-Alive")
      hi.sendOrderedMessage(request)

      val responseMessage = hi.receive.asInstanceOf[Message]
      val httpResponse = responseMessage.msg.asInstanceOf[DefaultHttpResponse]
      httpResponse.getStatus.getCode must_== OK
      httpResponse.getHeader(HttpHeaders.Names.CONTENT_LENGTH) must_== "0"
      hi.succeed

      hi.dontReceive // Must not close connection
    }

    "handle HTTP/1.1 requests with 'Connection: Close' headers" in testHandling(Action(Results.Ok)) { hi =>
      val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
      request.addHeader(CONNECTION, "Close")
      hi.sendOrderedMessage(request)

      val responseMessage = hi.receive.asInstanceOf[Message]
      val httpResponse = responseMessage.msg.asInstanceOf[DefaultHttpResponse]
      httpResponse.getStatus.getCode must_== OK
      httpResponse.getHeader(HttpHeaders.Names.CONTENT_LENGTH) must_== "0"
      hi.succeed

      hi.receive.asInstanceOf[Close.type]
      hi.dontReceive // Must not close connection twice
    }

    "handle HTTP/1.1 requests with 'Connection: Close' headers when an error occurs sending a response" in testHandling(Action(Results.Ok)) { hi =>
      val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
      request.addHeader(CONNECTION, "Close")
      hi.sendOrderedMessage(request)

      val responseMessage = hi.receive.asInstanceOf[Message]
      val httpResponse = responseMessage.msg.asInstanceOf[DefaultHttpResponse]
      httpResponse.getStatus.getCode must_== OK
      httpResponse.getHeader(HttpHeaders.Names.CONTENT_LENGTH) must_== "0"
      hi.fail(new Exception("Dummy exception sending"))

      hi.receive.asInstanceOf[Close.type]
      hi.dontReceive // Must not close connection twice
    }

    "handle HTTP/1.0 requests without Connection headers" in testHandling(Action(Results.Ok)) { hi =>
      val request = new DefaultHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/")
      hi.sendOrderedMessage(request)

      val responseMessage = hi.receive.asInstanceOf[Message]
      val httpResponse = responseMessage.msg.asInstanceOf[DefaultHttpResponse]
      httpResponse.getStatus.getCode must_== OK
      httpResponse.getHeader(HttpHeaders.Names.CONTENT_LENGTH) must_== "0"
      hi.succeed

      hi.receive.asInstanceOf[Close.type]
      hi.dontReceive // Must not close connection twice
    }

    "handle HTTP/1.0 requests with 'Connection: Keep-Alive' headers" in testHandling(Action(Results.Ok)) { hi =>
      val request = new DefaultHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/")
      request.addHeader(CONNECTION, "Keep-Alive")
      hi.sendOrderedMessage(request)

      val responseMessage = hi.receive.asInstanceOf[Message]
      val httpResponse = responseMessage.msg.asInstanceOf[DefaultHttpResponse]
      httpResponse.getStatus.getCode must_== OK
      httpResponse.getHeader(HttpHeaders.Names.CONTENT_LENGTH) must_== "0"
      hi.succeed

      hi.dontReceive // Must not close connection
    }

    "handle HTTP/1.0 requests with 'Connection: Close' headers" in testHandling(Action(Results.Ok)) { hi =>
      val request = new DefaultHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/")
      request.addHeader(CONNECTION, "Close")
      hi.sendOrderedMessage(request)

      val responseMessage = hi.receive.asInstanceOf[Message]
      val httpResponse = responseMessage.msg.asInstanceOf[DefaultHttpResponse]
      httpResponse.getStatus.getCode must_== OK
      httpResponse.getHeader(HttpHeaders.Names.CONTENT_LENGTH) must_== "0"
      hi.succeed

      hi.receive.asInstanceOf[Close.type]
      hi.dontReceive // Must not close connection twice
    }

  }

}