package play.api.mvc

import org.specs2.mutable._
import org.specs2.specification.{AroundOutside, Scope}
import org.specs2.execute.{Result => SpecsResult}

object BindingsSpec extends Specification {

  sequential

  "bindableString" should {
     val bindableString = PathBindable.bindableString

    "be able to bind a simple parameter" in {
       bindableString.bind("foo", "bar") must be_==(Right("bar"))
    }

    "be able to bind from a complex parameter" in {
       bindableString.bind("foo", "a%3A1%3A%7Bs%3A3%3A%22key%22%3Bs%3A7%3A%22%3F%3D+%28%29%26%40%22%3B%7D") must be_==(Right("""a:1:{s:3:"key";s:7:"?= ()&@";}"""))
    }

    "be idempotent on binding / unbinding" in {
       bindableString.unbind(
         "foo",
         bindableString.bind(
           "foo", 
           "a%3A1%3A%7Bs%3A3%3A%22key%22%3Bs%3A7%3A%22%3F%3D+%28%29%26%40%22%3B%7D"
         ).right.get
       ) must be_==("""a%3A1%3A%7Bs%3A3%3A%22key%22%3Bs%3A7%3A%22%3F%3D+%28%29%26%40%22%3B%7D""")
    }
  }
}
