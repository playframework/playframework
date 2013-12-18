package play.api.data.mapping

import org.specs2.mutable._
import scala.util.control.Exception._
import play.api.libs.functional._
import play.api.libs.functional.syntax._

object PathSpec extends Specification {
  "Path" should {
    "be compareable" in {
      (Path \ "foo" \ "bar") must equalTo((Path \ "foo" \ "bar"))
      (Path \ "foo" \ "bar").hashCode must equalTo((Path \ "foo" \ "bar").hashCode)
      (Path \ "foo" \ "bar") must not equalTo((Path \ "foo"))
      (Path \ "foo" \ "bar").hashCode must not equalTo((Path \ "foo").hashCode)
    }

    "compose" in {
      val c = (Path \ "foo" \ "bar") compose (Path \ "baz")
      val c2 = (Path \ "foo" \ "bar") ++ (Path \ "baz")
      c must equalTo(Path \ "foo" \ "bar" \ "baz")
      c2 must equalTo(Path \ "foo" \ "bar" \ "baz")
    }

    "have deconstructors" in {
      val path = Path \ "foo" \ "bar" \ "baz"

      val (h \: t) = path
      h must equalTo(Path \ "foo")
      t must equalTo(Path \ "bar" \ "baz")

      val (h1 \: h2 \: t2) = path
      h1 must equalTo(Path \ "foo")
      h2 must equalTo(Path \ "bar")
      t2 must equalTo(Path \ "baz")
    }
  }
}
