package play.api.libs.concurrent
import org.specs2.mutable._

object PromiseSpec extends Specification {

  "A Promise" should {
    "recover after an exception using recover" in {
      val promise = Promise[Int]()
      promise.redeem(6/0)
      
      promise.recover{ case e: ArithmeticException => 0 }
       .value.get must equalTo (0)
    }
  }
}
