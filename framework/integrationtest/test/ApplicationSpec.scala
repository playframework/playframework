package test
import org.specs2.mutable._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.test._
import play.api.test.MockApplication._

class FakeRequest[AnyContent] extends Request[AnyContent] {
  def uri = "foo"
  def method = "GET"
  def queryString = Map.empty()
  def body:AnyContent = AnyContentAsUrlFormEncoded(Map("foo"->Seq("value"))).asInstanceOf[AnyContent]

  def username = Some("peter")
  def path ="/"

  def headers = new Headers {
   def getAll(key: String) = Seq("testValue1","testValue2") 
   def keys = Set.empty
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
        val result = action.apply(new FakeRequest)
        val extracted = Extract.from(result)
        extracted._1.toString must equalTo("200")
        extracted._2.toString must equalTo("Map(Content-Type -> text/html; charset=utf-8)")
        extracted._3 must contain ("Hello world")
    }
  }
  
  "execute index1" in {
      withApplication(Nil, MockData.dataSource) {
        val action = controllers.Application.index()
        val result = action.apply(new FakeRequest)
        val extracted = Extract.from(result)
        extracted._1.toString must equalTo("200")
        extracted._2.toString must equalTo("Map(Content-Type -> text/html; charset=utf-8)")
        extracted._3 must contain ("Hello world")
    }
  } 
 } 
}
