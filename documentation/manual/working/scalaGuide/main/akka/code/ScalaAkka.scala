/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka {
  import java.io.File

  import scala.concurrent.duration._
  import scala.concurrent.Await

  import akka.actor.ActorSystem
  import akka.util.Timeout
  import play.api.mvc.ActionBuilder
  import play.api.mvc.AnyContent
  import play.api.mvc.DefaultActionBuilder
  import play.api.mvc.Request
  import play.api.test._

  class ScalaAkkaSpec extends PlaySpecification {
    sequential

    def withActorSystem[T](block: ActorSystem => T) = {
      val system = ActorSystem()
      try {
        block(system)
      } finally {
        system.terminate()
        Await.result(system.whenTerminated, Duration.Inf)
      }
    }

    override def defaultAwaitTimeout: Timeout = 5.seconds

    private def Action(implicit app: play.api.Application): ActionBuilder[Request, AnyContent] = {
      app.injector.instanceOf[DefaultActionBuilder]
    }

    "The Akka support" should {
      "allow injecting actors" in new WithApplication {
        import controllers._
        val controller = app.injector.instanceOf[Application]

        val helloActor = controller.helloActor
        // format: off
        import scala.concurrent.ExecutionContext.Implicits.global
        import actors.HelloActor.SayHello
        import play.api.mvc.Results._
        // format: on
        // #ask
        import scala.concurrent.duration._

        import akka.pattern.ask
        implicit val timeout: Timeout = 5.seconds

        def sayHello(name: String) = Action.async {
          (helloActor ? SayHello(name)).mapTo[String].map { message => Ok(message) }
        }
        // #ask

        contentAsString(sayHello("world")(FakeRequest())) must_== "Hello, world"
      }

      "allow binding actors" in new WithApplication(
        _.bindings(new modules.MyModule)
          .configure("my.config" -> "foo")
      ) {
        import injection._
        implicit val timeout: Timeout = 5.seconds
        val controller                = app.injector.instanceOf[Application]
        contentAsString(controller.getConfig(FakeRequest())) must_== "foo"
      }

      "allow binding actor factories" in new WithApplication(
        _.bindings(new factorymodules.MyModule)
          .configure("my.config" -> "foo")
      ) {
        import scala.concurrent.duration._

        import akka.actor._
        import akka.pattern.ask
        import play.api.inject.bind
        implicit val timeout: Timeout = 5.seconds

        import scala.concurrent.ExecutionContext.Implicits.global
        val actor = app.injector.instanceOf(bind[ActorRef].qualifiedWith("parent-actor"))
        val futureConfig = for {
          child  <- (actor ? actors.ParentActor.GetChild("my.config")).mapTo[ActorRef]
          config <- (child ? actors.ConfiguredChildActor.GetConfig).mapTo[String]
        } yield config
        await(futureConfig) must_== "foo"
      }
    }
  }

  package controllers {
//#controller
    import javax.inject._

    import actors.HelloActor
    import akka.actor._
    import play.api.mvc._

    @Singleton
    class Application @Inject() (system: ActorSystem, cc: ControllerComponents) extends AbstractController(cc) {
      val helloActor = system.actorOf(HelloActor.props, "hello-actor")

      // ...
    }
//#controller
  }

  package injection {
//#inject
    import javax.inject._

    import scala.concurrent.duration._
    import scala.concurrent.ExecutionContext

    import actors.ConfiguredActor._
    import akka.actor._
    import akka.pattern.ask
    import akka.util.Timeout
    import play.api.mvc._

    @Singleton
    class Application @Inject() (
        @Named("configured-actor") configuredActor: ActorRef,
        components: ControllerComponents
    )(
        implicit ec: ExecutionContext
    ) extends AbstractController(components) {
      implicit val timeout: Timeout = 5.seconds

      def getConfig = Action.async {
        (configuredActor ? GetConfig).mapTo[String].map { message => Ok(message) }
      }
    }
//#inject
  }

  package modules {
//#binding
    import actors.ConfiguredActor
    import com.google.inject.AbstractModule
    import play.api.libs.concurrent.AkkaGuiceSupport

    class MyModule extends AbstractModule with AkkaGuiceSupport {
      override def configure = {
        bindActor[ConfiguredActor]("configured-actor")
      }
    }
//#binding
  }

  package factorymodules {
//#factorybinding
    import actors._
    import com.google.inject.AbstractModule
    import play.api.libs.concurrent.AkkaGuiceSupport

    class MyModule extends AbstractModule with AkkaGuiceSupport {
      override def configure = {
        bindActor[ParentActor]("parent-actor")
        bindActorFactory[ConfiguredChildActor, ConfiguredChildActor.Factory]
      }
    }
//#factorybinding
  }

  package actors {
//#actor
    import akka.actor._

    object HelloActor {
      def props = Props[HelloActor]()

      case class SayHello(name: String)
    }

    class HelloActor extends Actor {
      import HelloActor._

      def receive = {
        case SayHello(name: String) =>
          sender() ! "Hello, " + name
      }
    }
//#actor

//#injected
    import javax.inject._

    import akka.actor._
    import play.api.Configuration

    object ConfiguredActor {
      case object GetConfig
    }

    class ConfiguredActor @Inject() (configuration: Configuration) extends Actor {
      import ConfiguredActor._

      val config = configuration.getOptional[String]("my.config").getOrElse("none")

      def receive = {
        case GetConfig =>
          sender() ! config
      }
    }
//#injected

//#injectedchild
    import javax.inject._

    import akka.actor._
    import com.google.inject.assistedinject.Assisted
    import play.api.Configuration

    object ConfiguredChildActor {
      case object GetConfig

      trait Factory {
        def apply(key: String): Actor
      }
    }

    class ConfiguredChildActor @Inject() (configuration: Configuration, @Assisted key: String) extends Actor {
      import ConfiguredChildActor._

      val config = configuration.getOptional[String](key).getOrElse("none")

      def receive = {
        case GetConfig =>
          sender() ! config
      }
    }
//#injectedchild

//#injectedparent
    import javax.inject._

    import akka.actor._
    import play.api.libs.concurrent.InjectedActorSupport

    object ParentActor {
      case class GetChild(key: String)
    }

    class ParentActor @Inject() (
        childFactory: ConfiguredChildActor.Factory
    ) extends Actor
        with InjectedActorSupport {
      import ParentActor._

      def receive = {
        case GetChild(key: String) =>
          val child: ActorRef = injectedChild(childFactory(key), key)
          sender() ! child
      }
    }
//#injectedparent
  }
}
