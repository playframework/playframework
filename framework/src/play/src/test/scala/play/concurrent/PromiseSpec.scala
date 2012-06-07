package play.concurrent

import org.specs2.mutable.Specification
import play.api.libs.concurrent._
import org.specs2.execute.Result
import play.api.libs.concurrent.execution.defaultContext

class PromiseSpec extends Specification {

  "Promise" can {

    "filter" in {

      "Redeemed values" << {
        val p = Promise.timeout(42, 100)
        p.filter(_ == 42).value.get must equalTo (42)
      }

      "Redeemed values not matching the predicate" << {
        val p = Promise.timeout(42, 100)
        p.filter(_ != 42).value.get must throwA [NoSuchElementException]
      }

      "Thrown values" << {
        val p = Promise.timeout(42, 100).map[Int]{ _ => throw new Exception("foo") }
        p.filter(_ => true).value.get must throwAn [Exception](message = "foo")
      }

    }

  }

}
