package play.concurrent

import org.specs2.mutable.{Around, Specification}
import play.api.libs.concurrent._
import org.specs2.execute.Result
import scala.concurrent.ExecutionContext.Implicits.global
import org.specs2.specification.Scope
import play.core.Invoker


class PromiseSpec extends Specification {

  "Promise" can {

    "filter" in new WithInvokerSystem {

      "Redeemed values" << {
        val p = Promise.timeout(42, 100)
        p.filter(_ == 42).value1.get must equalTo (42)
      }

      "Redeemed values not matching the predicate" << {
        val p = Promise.timeout(42, 100)
        p.filter(_ != 42).value1.get must throwA [NoSuchElementException]
      }

      "Thrown values" << {
        val p = Promise.timeout(42, 100).map[Int]{ _ => throw new Exception("foo") }
        p.filter(_ => true).value1.get must throwAn [Exception](message = "foo")
      }

    }

  }

  trait WithInvokerSystem extends Around with Scope {
    def around[T <% Result](t: => T) = {
      Invoker.start()
      try {
        t
      } finally {
        Invoker.reset()
      }
    }
  }

}
