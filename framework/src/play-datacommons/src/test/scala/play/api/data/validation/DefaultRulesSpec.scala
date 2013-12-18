package play.api.data.mapping

import org.specs2.mutable._
import scala.util.control.Exception._
import play.api.libs.functional._
import play.api.libs.functional.syntax._

object DefaultRulesSpec extends Specification {

  object R extends GenericRules
  import R._

  "DefaultRules" should {

    def failure(m: String, args: Any*) = Failure(Seq(Path -> Seq(ValidationError(m, args:_*))))

    "validate non emptyness" in {
      notEmpty.validate("foo") mustEqual(Success("foo"))
      notEmpty.validate("") mustEqual(failure("error.required"))
    }

    "validate min" in {
      min(4).validate(5) mustEqual(Success(5))
      min(4).validate(4) mustEqual(Success(4))
      min(4).validate(1) mustEqual(failure("error.min", 4))
      min(4).validate(-10) mustEqual(failure("error.min", 4))

      min("a").validate("b") mustEqual(Success("b"))
    }

    "validate max" in {
      max(8).validate(5) mustEqual(Success(5))
      max(5).validate(5) mustEqual(Success(5))
      max(0).validate(1) mustEqual(failure("error.max", 0))
      max(-30).validate(-10) mustEqual(failure("error.max", -30))
    }

  }
}
