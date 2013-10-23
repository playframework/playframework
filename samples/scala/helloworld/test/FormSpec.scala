import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class FormSpec extends Specification {
  
  import controllers.Application.helloForm
  
  "HelloWorld form" should {
    
    "require all fields" in {
      val form = helloForm.bind(Map.empty[String,String])
      
      form.hasErrors must beTrue
      form.errors.size must equalTo(2)
      
      form("name").hasErrors must beTrue
      form("repeat").hasErrors must beTrue
      form("color").hasErrors must beFalse
      
      form.value must beNone
    }
    
    "require name" in {
      val form = helloForm.bind(Map("repeat" -> "10", "color" -> "red"))
      
      form.hasErrors must beTrue
      form.errors.size must equalTo(1)
      
      form("name").hasErrors must beTrue
      form("repeat").hasErrors must beFalse
      form("color").hasErrors must beFalse
      
      form.data must havePair("color" -> "red")
      form.data must havePair("repeat" -> "10")
      
      form("repeat").value must beSome.which(_ == "10")
      form("color").value must beSome.which(_ == "red")
      form("name").value must beNone
      
      form.value must beNone
    }
    
    "validate repeat as numeric" in {
      val form = helloForm.bind(Map("name" -> "Bob", "repeat" -> "xx", "color" -> "red"))
      
      form.hasErrors must beTrue
      form.errors.size must equalTo(1)
      
      form("name").hasErrors must beFalse
      form("repeat").hasErrors must beTrue
      form("color").hasErrors must beFalse
      
      form.data must havePair("color" -> "red")
      form.data must havePair("repeat" -> "xx")
      form.data must havePair("name" -> "Bob")
      
      form("repeat").value must beSome.which(_ == "xx")
      form("color").value must beSome.which(_ == "red")
      form("name").value must beSome.which(_ == "Bob")
      
      form.value must beNone
    }
    
    "be filled" in {
      val form = helloForm.bind(Map("name" -> "Bob", "repeat" -> "10", "color" -> "red"))
      
      form.hasErrors must beFalse
      
      form.data must havePair("color" -> "red")
      form.data must havePair("repeat" -> "10")
      form.data must havePair("name" -> "Bob")
      
      form("repeat").value must beSome.which(_ == "10")
      form("color").value must beSome.which(_ == "red")
      form("name").value must beSome.which(_ == "Bob")
      
      form.value must beSome.which { _ match {
        case ("Bob", 10, Some("red")) => true
        case _ => false
      }}
    }
    
  }
  
}