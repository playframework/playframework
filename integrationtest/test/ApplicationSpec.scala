package test
import org.specs2.mutable._
import play.test.MockApplication
import play.api.mvc._
import play.api.libs.iteratee._

class MyGlobalApplicationMock extends MockApplication {
  def mockConfig: java.util.Map[String,String] = withMockDataSources
}

object ApplicationSpec extends Specification {

class FakeRequest[AnyContent] extends Request[AnyContent] {
  def uri = "foo"
  def method = "GET"
  def queryString = Map.empty()
  def body:AnyContent = AnyContent(Map("foo"->Seq("value"))).asInstanceOf[AnyContent]

  def username = Some("peter")
  def path ="/"

  def headers = new Headers {
   def getAll(key: String) = Seq("testValue1","testValue2") 
  }
  def cookies = new Cookies {
      def get(name: String) = Some(Cookie(name="foo",value="yay"))
  }
}
val globalMock = new MyGlobalApplicationMock()

"an Application" should {
"execute index" in {
  val action = controllers.Application.index()
  val bodyParser = action.parser
  val result = action.apply(new FakeRequest)
  result match {
    case SimpleResult(ResponseHeader(status, headers), body) => 
      status.toString must equalTo("200")
      headers.toString must equalTo("Map(Content-Type -> text/html)")
      body.toString must contain ("Enumerator")
    case _  => throw new Exception("it should have matched a valid response")
  }
 }
}
}
