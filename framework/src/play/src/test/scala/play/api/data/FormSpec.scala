package play.api.data

import Forms._
import org.specs2.mutable.Specification

object FormSpec extends Specification {

  "Form" should {

    "reject input if it contains global errors" in {
      Form( "value" -> nonEmptyText ).withGlobalError("some.error").bind( Map("value" -> "some value") ).fold(
        formWithErrors => { formWithErrors.errors.head.message must equalTo("some.error") },
        { data => "The mapping should fail." must equalTo("Error") }
      )
    }

  }
}