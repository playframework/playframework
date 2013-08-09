package play.api.data.validation

import org.specs2.mutable._
import scala.util.control.Exception._
import play.api.libs.functional._
import play.api.libs.functional.syntax._

object PathSpec extends Specification {

  "Path" should {
    val __ = Path[String]()

    "be compareable" in {
      (__ \ "foo" \ "bar") must equalTo((__ \ "foo" \ "bar"))
      (__ \ "foo" \ "bar").hashCode must equalTo((__ \ "foo" \ "bar").hashCode)
      (__ \ "foo" \ "bar") must not equalTo((__ \ "foo"))
      (__ \ "foo" \ "bar").hashCode must not equalTo((__ \ "foo").hashCode)
    }

    "compose" in {
      val c = (__ \ "foo" \ "bar") compose (__ \ "baz")
      val c2 = (__ \ "foo" \ "bar") ++ (__ \ "baz")
      c must equalTo(__ \ "foo" \ "bar" \ "baz")
      c2 must equalTo(__ \ "foo" \ "bar" \ "baz")
    }

    "have deconstructors" in {
      val path = __ \ "foo" \ "bar" \ "baz"

      val (h \: t) = path
      h must equalTo(KeyPathNode("foo"))
      t must equalTo(__ \ "bar" \ "baz")

      val (h1 \: h2 \: t2) = path
      h1 must equalTo(KeyPathNode("foo"))
      h2 must equalTo(KeyPathNode("bar"))
      t2 must equalTo(__ \ "baz")
    }

  }
}
