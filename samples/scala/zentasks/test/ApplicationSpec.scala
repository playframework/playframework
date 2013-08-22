import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {
  
  
  "Application" should {
    
    
    "go to login page without credentials" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        val result  = route( FakeRequest( GET, "/")).get
        status(result) must equalTo(303)
      }      
    }
    "list the secured product page with credentials" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
        val result  = route( FakeRequest( GET, "/").withSession("email"->"guillaume@sample.com")).get
        status(result) must equalTo(200)
      }      
    }
    
  }
}
