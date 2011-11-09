package test
import org.specs2.mutable._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Promise
import play.api.test.MockData
import play.api.test.MockApplication._

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

object ApplicationSpec extends Specification {

"an Application" should {
  "execute index" in {
      withApplication(Nil, MockData.dataSource) {
        val action = controllers.Application.index()
        val bodyParser = action.parser
        val result = action.apply(new FakeRequest)
        result match {
          case simp @ SimpleResult(ResponseHeader(status, headers), body) => 
            status.toString must equalTo("200")
            headers.toString must equalTo("Map(Content-Type -> text/html)")
            body.toString must contain ("Enumerator")
            val later:Promise[String] = (Iteratee.fold[simp.BODY_CONTENT,String](""){ case (s,e) => s+e } <<: body).flatMap(_.run)
            later.onRedeem{body=>
              body must contain ("Hello world")
            }
          case _  => throw new Exception("it should have matched a valid response")
       }
    }
  }
 }
}
