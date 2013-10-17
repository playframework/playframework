package play.it.concurrent

import play.api.test._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.concurrent.Promise


class PromiseSpec extends PlaySpecification {

  "Promise" can {

    "Redeemed values" in new WithApplication() {
      val p = Promise.timeout(42, 100)
      await(p.filter(_ == 42)) must equalTo (42)
    }

    "Redeemed values not matching the predicate" in new WithApplication() {
      val p = Promise.timeout(42, 100)
      await(p.filter(_ != 42)) must throwA [NoSuchElementException]
    }

    "Thrown values" in new WithApplication() {
      val p = Promise.timeout(42, 100).map[Int]{ _ => throw new Exception("foo") }
      await(p.filter(_ => true)) must throwAn [Exception](message = "foo")
    }

  }

  "Promise timeouts" should {

    "yield their message" in new WithApplication() {
      val future = Promise.timeout("hello", 10)
      await(future) must_== "hello"
    }

    "yield any exceptions thrown when generating a message" in new WithApplication() {
      val future = Promise.timeout[Unit](throw new Exception("error!"), 10)
      await(future) must throwAn[Exception](message = "error!")
    }

  }

}
