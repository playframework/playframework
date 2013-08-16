package play.core.server

import org.junit.{Rule, Test}

import org.databene.contiperf.PerfTest
import org.databene.contiperf.junit.ContiPerfRule
import org.junit.rules.{TemporaryFolder, TestRule}
import org.junit.runners.model.{FrameworkMethod, Statement}
import org.junit.runner.Description
import org.jboss.netty.handler.codec.http._
import scala.util.{ Try, Success }
import scala.concurrent.{Promise, Future, Await}
import scala.concurrent.duration._
import scala.language.postfixOps
import play.core.server.netty.PlayDefaultUpstreamHandler
import com.typesafe.netty.http.pipelining.{OrderedDownstreamChannelEvent, OrderedUpstreamMessageEvent}
import org.jboss.netty.channel._
import play.core._
import play.api.{Play, DefaultApplication, Application, Mode}
import java.io.File
import java.net.{InetSocketAddress, SocketAddress}
import org.databene.contiperf.report.CSVSummaryReportModule
import play.core.Router.{HandlerDef, Route, Routes}
import play.core.PathPattern
import play.core.StaticPart
import play.api.mvc.{Action, Controller, Handler, RequestHeader}

/**
 * Exercises the performance of Play given the exclusion of Netty. Results from these tests can be captured and tracked.
 */
class ServerBenchmark {

  // Tests

  @Test
  @PerfTest(threads = 1, duration = 35000, warmUp = 30000)
  def makeHelloWordRequest() {
    for (i <- 1 to 100) {
      val f = withDefaultUpstreamHandler(SimpleRequest)
      Await.ready(f, 2 seconds)
    }
  }


  // JUnit rules

  @Rule
  def r = new TestRule {
    val cpr = new ContiPerfRule(new CSVSummaryReportModule)

    override def apply(base: Statement, description: Description): Statement = {
      cpr.apply(
        base,
        new FrameworkMethod(ServerBenchmark.this.getClass.getMethod(description.getMethodName)),
        ServerBenchmark.this)
    }
  }

  @Rule
  def tempFolder = {
    val f = new TemporaryFolder
    f.create()
    f
  }


  // Test fixtures

  val SimpleRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
  SimpleRequest.addHeader("Host", "localhost")
  SimpleRequest.addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) Gecko/20130501 Firefox/30.0 AppleWebKit/600.00 Chrome/30.0.0000.0 Trident/10.0 Safari/600.00")
  SimpleRequest.addHeader("Cookie", "uid=12345678901234567890; __utma=1.1234567890.1234567890.1234567890.1234567890.12; wd=2560x1600")
  SimpleRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
  SimpleRequest.addHeader("Accept-Language", "en-US,en;q=0.5")
  SimpleRequest.addHeader("Connection", "keep-alive")

  object HelloWorldApp extends Controller {
    def helloWorld = Action {
      Ok("Hello World!")
    }
  }

  object Routes extends Router.Routes {

    private var _prefix = "/"

    def setPrefix(prefix: String) {
      _prefix = prefix
      List[(String, Routes)]().foreach {
        case (p, router) => router.setPrefix(prefix + (if (prefix.endsWith("/")) "" else "/") + p)
      }
    }

    def prefix = _prefix

    lazy val defaultPrefix = {
      if (Routes.prefix.endsWith("/")) "" else "/"
    }

    private[this] lazy val hello_world = Route("GET", PathPattern(List(StaticPart(Routes.prefix))))

    def documentation = List(( """GET""", prefix, """hello_world""")).foldLeft(List.empty[(String, String, String)]) {
      (s, e) => e.asInstanceOf[Any] match {
        case r@(_, _, _) => s :+ r.asInstanceOf[(String, String, String)]
        case l => s ++ l.asInstanceOf[List[(String, String, String)]]
      }
    }

    def routes: PartialFunction[RequestHeader, Handler] = {
      case hello_world(params) => {
        call {
          invokeHandler(HelloWorldApp.helloWorld, HandlerDef(this, "hello_world", "index", Nil, "GET", """ Home page""", Routes.prefix + """"""))
        }
      }

    }

  }

  val application = new DefaultApplication(new File("."), this.getClass.getClassLoader, None, Mode.Test) {
    override protected def loadRoutes: Option[Router.Routes] = Some(Routes)
  }

  Play.start(application)

  val ap = new ApplicationProvider {
    def get: Try[Application] = Success(application)

    def path: File = tempFolder.getRoot
  }

  val server = new Server() {
    def mode: Mode.Mode = Mode.Test

    def applicationProvider: ApplicationProvider = ap
  }

  val upstreamHandler = new PlayDefaultUpstreamHandler(server: Server, null)

  class StubChannelHandlerContext(rp: Promise[HttpResponse]) extends ChannelHandlerContext {
    def getChannel: Channel = ???

    def getPipeline: ChannelPipeline = ???

    def getName: String = ???

    def getHandler: ChannelHandler = ???

    def canHandleUpstream: Boolean = ???

    def canHandleDownstream: Boolean = ???

    def sendUpstream(e: ChannelEvent) {}

    def sendDownstream(e: ChannelEvent) = {
      val ode = e.asInstanceOf[OrderedDownstreamChannelEvent]
      val ce = ode.getChannelEvent
      val me = ce.asInstanceOf[MessageEvent]
      rp.success(me.getMessage.asInstanceOf[HttpResponse])
    }

    def getAttachment: AnyRef = ???

    def setAttachment(attachment: Any) {}
  }

  val channel = new Channel {
    val remoteAddress = new InetSocketAddress(8080)

    def getId: Integer = ???

    def getFactory: ChannelFactory = ???

    def getParent: Channel = ???

    def getConfig: ChannelConfig = ???

    def getPipeline: ChannelPipeline = ???

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

    def close(): ChannelFuture = ???

    def getCloseFuture: ChannelFuture = ???

    def getInterestOps: Int = ???

    def isReadable: Boolean = ???

    def isWritable: Boolean = ???

    def setInterestOps(interestOps: Int): ChannelFuture = ???

    def setReadable(readable: Boolean): ChannelFuture = ???

    def getAttachment: AnyRef = ???

    def setAttachment(attachment: Any) {}

    def compareTo(o: Channel): Int = ???
  }

  def withDefaultUpstreamHandler(request: HttpRequest): Future[HttpResponse] = {
    val rp = Promise[HttpResponse]
    val ctx = new StubChannelHandlerContext(rp)
    upstreamHandler.messageReceived(ctx, new OrderedUpstreamMessageEvent(0, channel, request, null))
    rp.future
  }

}
