package play.data

import org.specs2.mutable.Specification

/**
 * Specs for Java dynamic forms
 */
object DynamicFormSpec extends Specification {
  "a dynamic form" should {

    "bind values from a request" in {
      val form = new DynamicForm().bindFromRequest(new DummyRequest(Map("foo" -> Array("bar"))))
      form.get("foo") must_== "bar"
    }

    "allow access to raw data values from request" in {
      val form = new DynamicForm().bindFromRequest(new DummyRequest(Map("foo" -> Array("bar"))))
      form.data().get("foo") must_== "bar"
    }

  }
}