/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http

import com.typesafe.netty.http.pipelining.{OrderedDownstreamChannelEvent, OrderedUpstreamMessageEvent}
import java.io.File
import java.net.{ InetSocketAddress, SocketAddress }
import java.util.concurrent.{ CountDownLatch, TimeUnit }
import org.databene.contiperf.junit.ContiPerfRule
import org.databene.contiperf.PerfTest
import org.databene.contiperf.report.CSVSummaryReportModule
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.junit.runner.Description
import org.junit.runners.model.{FrameworkMethod, Statement}
import org.junit.{Rule, Test}
import org.junit.rules.TestRule
import play.api.mvc.{Action, Controller, Handler, RequestHeader}
import play.api.{Play, DefaultApplication, Mode}
import play.core._
import play.core.Router.{HandlerDef, Route, Routes}
import play.core.{PathPattern, StaticPart}
import scala.concurrent.duration._

import scala.language.postfixOps

/**
 * Exercises the performance of Play given the exclusion of Netty. Results from these tests can be captured and tracked.
 */
class ServerBenchmark extends NettyRunners {

  // Tests

  @Test
  @PerfTest(threads = 1, duration = 35000, warmUp = 30000)
  def makeHelloWordRequest() {
    val application = new DefaultApplication(new File("."), this.getClass.getClassLoader, None, Mode.Test) {
      override protected def loadRoutes: Option[Router.Routes] = Some(Routes)
    }

    val remoteAddress = new InetSocketAddress(8080)

    class LatchedChannel(pipeline: ChannelPipeline, remoteAddress: SocketAddress) extends StubChannel(pipeline, remoteAddress) {
      val responseLatch = new CountDownLatch(1)
    }

    val downstreamHandler = new ChannelDownstreamHandler {
      def handleDownstream(ctx: ChannelHandlerContext, e: ChannelEvent) = e match {
        case ode: OrderedDownstreamChannelEvent => ode.getChannelEvent match {
          case me: MessageEvent => me.getMessage match {
            case hr: HttpResponse => {
              val latchedChannel = e.getChannel.asInstanceOf[LatchedChannel]
              latchedChannel.responseLatch.countDown()
              latchedChannel.getCloseFuture.setSuccess() // Netty uses this future to clean up after the channel
            }
          }
        }
      }
    }
    withDownstreamHandler(downstreamHandler, application) { pipeline =>
      val latchedChannel = new LatchedChannel(pipeline, remoteAddress)
      for (i <- 1 to 100) {
        pipeline.sendUpstream(new OrderedUpstreamMessageEvent(0, latchedChannel, SimpleRequest, remoteAddress))
        latchedChannel.responseLatch.await(2, TimeUnit.SECONDS)
      }
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

}
