/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.tests.webservice

package client {
//#client
  import javax.inject.Inject

  import scala.concurrent.ExecutionContext
  import scala.concurrent.Future

  import play.api.libs.ws.WSClient

  class GitHubClient(ws: WSClient, baseUrl: String)(implicit ec: ExecutionContext) {
    @Inject def this(ws: WSClient, ec: ExecutionContext) = this(ws, "https://api.github.com")(ec)

    def repositories(): Future[Seq[String]] = {
      ws.url(baseUrl + "/repositories").get().map { response => (response.json \\ "full_name").map(_.as[String]).toSeq }
    }
  }
//#client
}

package test {
  // format: off
  import client._
  // format: on
//#full-test
  import scala.concurrent.duration._
  import scala.concurrent.Await

  import org.specs2.mutable.Specification
  import play.api.libs.json._
  import play.api.mvc._
  import play.api.routing.sird._
  import play.api.test._
  import play.core.server.Server

  class GitHubClientSpec extends Specification {
    import scala.concurrent.ExecutionContext.Implicits.global

    "GitHubClient" should {
      "get all repositories" in {
        Server.withRouterFromComponents() { components =>
          import Results._
          import components.{ defaultActionBuilder => Action }
          {
            case GET(p"/repositories") =>
              Action {
                Ok(Json.arr(Json.obj("full_name" -> "octocat/Hello-World")))
              }
          }
        } { implicit port =>
          WsTestClient.withClient { client =>
            val result = Await.result(new GitHubClient(client, "").repositories(), 10.seconds)
            result must_== Seq("octocat/Hello-World")
          }
        }
      }
    }
  }
//#full-test
}

import scala.concurrent.duration._
import scala.concurrent.Await

import client._
import org.specs2.mutable.Specification
import play.api.routing.sird._
import play.api.routing.Router
import play.api.BuiltInComponentsFromContext
import play.filters.HttpFiltersComponents

class ScalaTestingWebServiceClients extends Specification {
  import scala.concurrent.ExecutionContext.Implicits.global

  "webservice testing" should {
    "allow mocking a service" in {
      // #mock-service
      import play.api.libs.json._
      import play.api.mvc._
      import play.api.routing.sird._
      import play.core.server.Server

      Server.withRouterFromComponents() { components =>
        import Results._
        import components.{ defaultActionBuilder => Action }
        {
          case GET(p"/repositories") =>
            Action {
              Ok(Json.arr(Json.obj("full_name" -> "octocat/Hello-World")))
            }
        }
      } { implicit port =>
        // #mock-service
        ok
      }
    }

    "allow sending a resource" in {
      // #send-resource
      import play.api.mvc._
      import play.api.routing.sird._
      import play.api.test._
      import play.core.server.Server

      Server.withApplicationFromContext() { context =>
        new BuiltInComponentsFromContext(context) with HttpFiltersComponents {
          override def router: Router = Router.from {
            case GET(p"/repositories") =>
              Action { req => Results.Ok.sendResource("github/repositories.json")(executionContext, fileMimeTypes) }
          }
        }.application
      } { implicit port =>
        // #send-resource
        WsTestClient.withClient { client =>
          Await.result(new GitHubClient(client, "").repositories(), 10.seconds) must_== Seq("octocat/Hello-World")
        }
      }
    }

    "allow being dry" in {
      // #with-github-client
      import play.api.mvc._
      import play.api.routing.sird._
      import play.core.server.Server
      import play.api.test._

      def withGitHubClient[T](block: GitHubClient => T): T = {
        Server.withApplicationFromContext() { context =>
          new BuiltInComponentsFromContext(context) with HttpFiltersComponents {
            override def router: Router = Router.from {
              case GET(p"/repositories") =>
                Action { req => Results.Ok.sendResource("github/repositories.json")(executionContext, fileMimeTypes) }
            }
          }.application
        } { implicit port => WsTestClient.withClient { client => block(new GitHubClient(client, "")) } }
      }
      // #with-github-client

      // #with-github-test
      withGitHubClient { client =>
        val result = Await.result(client.repositories(), 10.seconds)
        result must_== Seq("octocat/Hello-World")
      }
      // #with-github-test
    }
  }
}
