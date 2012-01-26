package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

import play.api.data._
import play.api.data.Forms._

class FormSpec extends Specification {
  
  val userForm = Form(
    tuple(
      "email" -> text,
      "address" -> optional(
        single("city" -> nonEmptyText)
      )
    )
  )
  
  "the userForm" should {
  
    "don't bind the address if missing" in {
      
      val (email, address) = userForm.bind(
        Map("email" -> "coco")
      ).get
      
      email must equalTo("coco")
      address must beNone
      
    }
    
    "don't bind the address if blank" in {
      
      val (email, address) = userForm.bind(
        Map("email" -> "coco", "address.city" -> "")
      ).get
      
      email must equalTo("coco")
      address must beNone
      
    }
    
    "bind the address" in {
      
      val (email, address) = userForm.bind(
        Map("email" -> "coco", "address.city" -> "Paris")
      ).get
      
      email must equalTo("coco")
      address must beSome.which(_ == "Paris")
      
    }
  
  }
  
}