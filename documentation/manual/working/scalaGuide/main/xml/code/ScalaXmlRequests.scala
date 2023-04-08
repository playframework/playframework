/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.xml.scalaxmlrequests {
  import scala.language.postfixOps
  import scala.xml.NodeSeq

  import org.junit.runner.RunWith
  import org.specs2.runner.JUnitRunner
  import play.api.mvc._
  import play.api.mvc.Results._
  import play.api.test._
  import play.api.Application

  @RunWith(classOf[JUnitRunner])
  class ScalaXmlRequestsSpec extends PlaySpecification {
    private def parse(implicit app: Application) = app.injector.instanceOf(classOf[PlayBodyParsers])
    private def Action[A](block: Request[AnyContent] => Result)(implicit app: Application) =
      app.injector.instanceOf(classOf[DefaultActionBuilder]).apply(block)
    private def Action(bodyParser: BodyParser[NodeSeq])(block: Request[NodeSeq] => Result)(implicit app: Application) =
      app.injector.instanceOf(classOf[DefaultActionBuilder])(parse.xml).apply(block)

    "A scala XML request" should {
      "request body as xml" in new WithApplication {
        override def running() = {
          // #xml-request-body-asXml
          def sayHello = Action { request =>
            request.body.asXml
              .map { xml =>
                (xml \\ "name" headOption)
                .map(_.text)
                .map { name => Ok("Hello " + name) }
                .getOrElse {
                  BadRequest("Missing parameter [name]")
                }
              }
              .getOrElse {
                BadRequest("Expecting Xml data")
              }
          }

          // #xml-request-body-asXml

          val request = FakeRequest().withXmlBody(<name>XF</name>).map(_.xml)
          status(call(sayHello, request)) must beEqualTo(Helpers.OK)
        }
      }

      "request body as xml body parser" in new WithApplication {
        override def running() = {
          // #xml-request-body-parser
          def sayHello = Action(parse.xml) { request =>
            (request.body \\ "name" headOption)
            .map(_.text)
            .map { name => Ok("Hello " + name) }
            .getOrElse {
              BadRequest("Missing parameter [name]")
            }
          }

          // #xml-request-body-parser

          val request = FakeRequest().withXmlBody(<name>XF</name>).map(_.xml)
          status(call(sayHello, request)) must beEqualTo(Helpers.OK)
        }
      }

      "request body as xml body parser and xml response" in new WithApplication {
        override def running() = {
          // #xml-request-body-parser-xml-response
          def sayHello = Action(parse.xml) { request =>
            (request.body \\ "name" headOption)
            .map(_.text)
            .map { name =>
              Ok(<message status="OK">Hello
                  {name}
                </message>)
            }
            .getOrElse {
              BadRequest(<message status="KO">Missing parameter [name]</message>)
            }
          }

          // #xml-request-body-parser-xml-response

          val request = FakeRequest().withXmlBody(<name>XF</name>).map(_.xml)
          status(call(sayHello, request)) must beEqualTo(Helpers.OK)
        }
      }
    }
  }
}
