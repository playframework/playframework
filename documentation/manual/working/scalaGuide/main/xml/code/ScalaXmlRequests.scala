/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.xml.scalaxmlrequests {

  import play.api.test._
  import org.junit.runner.RunWith
  import org.specs2.runner.JUnitRunner
  import play.api.Application
  import play.api.mvc._
  import play.api.mvc.Results._

  import scala.xml.NodeSeq

  @RunWith(classOf[JUnitRunner])
  class ScalaXmlRequestsSpec extends PlaySpecification {

    private def parse(implicit app: Application) = app.injector.instanceOf(classOf[PlayBodyParsers])
    private def Action[A](block: Request[AnyContent] => Result)(implicit app: Application) =
      app.injector.instanceOf(classOf[DefaultActionBuilder]).apply(block)
    private def Action(bodyParser: BodyParser[NodeSeq])(block: Request[NodeSeq] => Result)(implicit app: Application) =
      app.injector.instanceOf(classOf[DefaultActionBuilder])(parse.xml).apply(block)

    "A scala XML request" should {

      "request body as xml" in new WithApplication {

        //#xml-request-body-asXml
        def sayHello = Action { request =>
          request.body.asXml
            .map { xml =>
              (xml \\ "name" headOption)
                .map(_.text)
                .map { name =>
                  Ok("Hello " + name)
                }
                .getOrElse {
                  BadRequest("Missing parameter [name]")
                }
            }
            .getOrElse {
              BadRequest("Expecting Xml data")
            }
        }

        //#xml-request-body-asXml

        private val request = FakeRequest().withXmlBody(<name>XF</name>).map(_.xml)
        status(call(sayHello, request)) must beEqualTo(Helpers.OK)
      }

      "request body as xml body parser" in new WithApplication {

        //#xml-request-body-parser
        def sayHello = Action(parse.xml) { request =>
          (request.body \\ "name" headOption)
            .map(_.text)
            .map { name =>
              Ok("Hello " + name)
            }
            .getOrElse {
              BadRequest("Missing parameter [name]")
            }
        }

        //#xml-request-body-parser

        private val request = FakeRequest().withXmlBody(<name>XF</name>).map(_.xml)
        status(call(sayHello, request)) must beEqualTo(Helpers.OK)
      }

      "request body as xml body parser and xml response" in new WithApplication {

        //#xml-request-body-parser-xml-response
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

        //#xml-request-body-parser-xml-response

        private val request = FakeRequest().withXmlBody(<name>XF</name>).map(_.xml)
        status(call(sayHello, request)) must beEqualTo(Helpers.OK)
      }
    }
  }
}
