package play.it.http.parsingdeferred

import java.util.concurrent.CompletableFuture

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.stream.javadsl.{ Sink => JSink }
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import org.specs2.specification.core.Fragment
import org.specs2.specification.AfterAll
import org.specs2.specification.AfterEach
import org.specs2.specification.BeforeAll
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.streams.Accumulator
import play.api.libs.ws.WSResponse
import play.api.mvc.Handler.Stage
import play.api.mvc._
import play.api.routing.HandlerDef
import play.api.routing.Router
import play.api.test.Helpers
import play.api.test.PlaySpecification
import play.api.test.WsTestClient
import play.core.server.Attrs.DeferredBodyParserInvoker
import play.it.http.JAction
import play.it.http.MockController
import play.it.AkkaHttpIntegrationSpecification
import play.it.NettyIntegrationSpecification
import play.it.ServerIntegrationSpecification
import play.libs.F
import play.libs.streams.{ Accumulator => JAccumulator }
import play.mvc.Http.{ Request => JRequest }
import play.mvc.Http
import play.mvc.Results
import play.mvc.{ BodyParser => JBodyParser }
import play.mvc.{ Result => JResult }

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class AkkaHTTPServerDeferredBodyParsingSpec extends DeferredBodyParsingSpec with AkkaHttpIntegrationSpecification {
  override def serverBackend(): String = "akka-http"
}

class NettyServerDeferredBodyParsingSpec extends DeferredBodyParsingSpec with NettyIntegrationSpecification {
  override def serverBackend(): String = "netty"
}

object DeferredBodyParsingSpec {

  // Not possible to put inside the trait, Guice complains:
  // "Injecting into inner classes is not supported.  Please use a 'static' class (top-level or nested)..."
  class SimpleJavaBodyParser extends JBodyParser[String] {
    override def apply(request: Http.RequestHeader): JAccumulator[ByteString, F.Either[JResult, String]] =
      JAccumulator.strict[ByteString, F.Either[JResult, String]](
        bytesOpt =>
          CompletableFuture.completedFuture(
            F.Either.Right(
              bytesOpt.map[String](bytes => buildParserDebugMessage(request.asScala(), bytes.utf8String)).orElse("")
            )
          ),
        JSink
          .fold[ByteString, ByteString](ByteString.empty, (state, bs) => state ++ bs)
          .mapMaterializedValue(
            _.thenApply(bytes => F.Either.Right(buildParserDebugMessage(request.asScala(), bytes.utf8String)))
          )
      )
  }

  // These are the values we are checking against
  val notDeferredBodyContent =
    "Action composition, body was parsed already: true, internal request attribute set: false | Body parsed: abc, request attribute set: false, internal request attribute set: false"
  val deferredBodyContent =
    "Action composition, body was parsed already: false, internal request attribute set: true | Body parsed: abc, request attribute set: true, internal request attribute set: false"

  def buildParserDebugMessage(request: RequestHeader, parsedBody: String) =
    s"Body parsed: $parsedBody, request attribute set: ${request.attrs.contains(Attrs.REQUEST_FLOW.asScala())}, internal request attribute set: ${request.attrs
      .contains(DeferredBodyParserInvoker)}"

  def buildActionCompositionMessage(request: Request[_]) =
    s"Action composition, body was parsed already: ${(request.body != null)}, internal request attribute set: ${request.attrs
      .contains(DeferredBodyParserInvoker)}"
}

trait DeferredBodyParsingSpec
    extends PlaySpecification
    with WsTestClient
    with ServerIntegrationSpecification
    with BeforeAll
    with AfterAll
    with AfterEach {

  sequential

  def serverBackend(): String

  override def beforeAll(): Unit = {
    // Let's set the server header for both backends, so we can test later if the correct server backend is used
    System.setProperty("play.server.akka.server-header", "akka-http-server-backend")
    System.setProperty("play.server.netty.server-header", "netty-server-backend")
  }

  override def afterAll(): Unit = {
    System.clearProperty("play.server.akka.server-header")
    System.clearProperty("play.server.netty.server-header")
    ConfigFactory.invalidateCaches()
  }

  protected override def after: Any = {
    System.clearProperty("play.server.deferBodyParsing")
  }

  // ### General

  def makeGenericRequest[T](
      handler: Application => Handler,
      deferBodyParsing: Option[Boolean] = None,
      routesModifiers: Seq[String] = Seq.empty
  )(block: WSResponse => T): T = {
    lazy val app: Application = GuiceApplicationBuilder()
      .routes {
        case _ =>
          new Stage {
            override def apply(requestHeader: RequestHeader): (RequestHeader, Handler) = (
              requestHeader.addAttr(
                Router.Attrs.HandlerDef,
                HandlerDef(null, null, null, null, null, null, null, null, routesModifiers)
              ),
              handler(app)
            )
          }
      }
      .build()

    deferBodyParsing.foreach(defer => System.setProperty("play.server.deferBodyParsing", defer.toString))
    ConfigFactory.invalidateCaches()

    implicit val port = testServerPort
    running(TestServer(port, app)) {
      val response = await(wsUrl("/").post("abc"))
      // Just make 100% sure we run all tests with both akka-http and netty server backend
      response.header("server") must beSome(serverBackend() + "-server-backend")
      block(response)
    }
  }

  // ### Scala API

  import DeferredBodyParsingSpec._

  val system               = ActorSystem()
  val mat                  = Materializer.matFromSystem(system)
  val ec: ExecutionContext = system.dispatcher

  val simpleScalaBodyParser: BodyParser[String] = BodyParser { request =>
    Accumulator.strict[ByteString, Either[Result, String]](
      bytesOpt =>
        Future.successful(
          Right(bytesOpt.map(bytes => buildParserDebugMessage(request, bytes.utf8String)).getOrElse(""))
        ),
      Sink
        .fold[ByteString, ByteString](ByteString.empty)((state, bs) => state ++ bs)
        .mapMaterializedValue(_.map(bytes => Right(buildParserDebugMessage(request, bytes.utf8String)))(ec))
    )
  }

  class SimpleScalaAction(parser: BodyParser[String])(implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) {
    override def invokeBlock[A](req: Request[A], block: Request[A] => Future[Result]) =
      parseBody(req.addAttr(Attrs.REQUEST_FLOW.asScala(), buildActionCompositionMessage(req)), block)
  }

  class ScalaMockController(simpleScalaAction: SimpleScalaAction, cc: ControllerComponents)
      extends AbstractController(cc) {
    def index = simpleScalaAction { request =>
      Ok(request.attrs.get(Attrs.REQUEST_FLOW.asScala()).getOrElse("") + " | " + request.body)
    }
  }

  val simpleScalaAction = new SimpleScalaAction(simpleScalaBodyParser)(ec)

  def makeScalaRequest[T](deferBodyParsing: Option[Boolean] = None, routesModifiers: Seq[String] = Seq.empty)(
      block: WSResponse => T
  ): T =
    makeGenericRequest(
      _ => new ScalaMockController(simpleScalaAction, Helpers.stubControllerComponents()).index,
      deferBodyParsing,
      routesModifiers
    )(block)

  // ### Java API

  private def jActionController() = {
    new MockController {
      @SimpleActionAnnotation
      @JBodyParser.Of(classOf[SimpleJavaBodyParser])
      override def action(request: JRequest): JResult =
        Results.ok(request.attrs().get[String](Attrs.REQUEST_FLOW) + " | " + request.body().asText())
    }
  }

  def makeJavaRequest[T](deferBodyParsing: Option[Boolean] = None, routesModifiers: Seq[String] = Seq.empty)(
      block: WSResponse => T
  ): T =
    makeGenericRequest(JAction(_, jActionController()), deferBodyParsing, routesModifiers)(block)

  // Finally tests

  def makeTestRequest[T](
      apiKey: String,
      deferBodyParsing: Option[Boolean] = None,
      routesModifiers: Seq[String] = Seq.empty
  )(block: WSResponse => T): T =
    if (apiKey == "Scala") {
      makeScalaRequest(deferBodyParsing, routesModifiers)(block)
    } else if (apiKey == "Java") {
      makeJavaRequest(deferBodyParsing, routesModifiers)(block)
    } else {
      throw new RuntimeException("Don't make typos. It's either 'Scala' or 'Java'")
    }

  Fragment.foreach(Seq("Scala", "Java")) { apiKey =>
    s"$apiKey API" should {
      "by default not defer body parsing" in makeTestRequest(apiKey) { response =>
        response.body must beEqualTo(notDeferredBodyContent)
      }
      "defer body parsing when activated globally via config" in makeTestRequest(
        apiKey,
        deferBodyParsing = Some(true)
      ) { response =>
        response.body must beEqualTo(deferredBodyContent)
      }
      "not defer body parsing when explicitly deactivated globally via config" in makeTestRequest(
        apiKey,
        Some(false)
      ) { response =>
        response.body must beEqualTo(notDeferredBodyContent)
      }
      "defer body parsing when deactivated globally but activated via route modifier" in makeTestRequest(
        apiKey,
        routesModifiers = Seq("deferBodyParsing")
      ) { response =>
        response.body must beEqualTo(deferredBodyContent)
      }
      "defer body parsing when deactivated globally but activated via case insensitive route modifier" in makeTestRequest(
        apiKey,
        routesModifiers = Seq("dEfErBOdyPaRSING")
      ) { response =>
        response.body must beEqualTo(deferredBodyContent)
      }
      "not defer body parsing when activated globally but deactivated via route modifier" in makeTestRequest(
        apiKey,
        Some(true),
        routesModifiers = Seq("dontDeferBodyParsing")
      ) { response =>
        response.body must beEqualTo(notDeferredBodyContent)
      }
      "not defer body parsing when activated globally but deactivated via case insensitive route modifier" in makeTestRequest(
        apiKey,
        Some(true),
        routesModifiers = Seq("doNTDeFErBoDyPARSING")
      ) { response =>
        response.body must beEqualTo(notDeferredBodyContent)
      }
      "not defer body parsing when activated globally and also via route modifier but deactivated via route modifier" in makeTestRequest(
        apiKey,
        Some(true),
        routesModifiers = Seq("deferBodyParsing", "dontDeferBodyParsing")
      ) { response =>
        response.body must beEqualTo(notDeferredBodyContent)
      }
      "not defer body parsing when deactivated globally and activated via route modifier but deactivated via route modifier" in makeTestRequest(
        apiKey,
        routesModifiers = Seq("deferBodyParsing", "dontDeferBodyParsing")
      ) { response =>
        response.body must beEqualTo(notDeferredBodyContent)
      }
    }
  }
}
