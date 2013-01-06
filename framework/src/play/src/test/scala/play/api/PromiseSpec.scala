package play.api.libs.concurrent
import org.specs2.mutable._
import scala.concurrent.ExecutionContext.Implicits.global

object PromiseSpec extends Specification {

  "A Promise" should {
    "recover after an exception using recover" in {
      val promise = Promise[Int]()
      promise.redeem(6/0)
      
<<<<<<< .merge_file_2PHnZn
      promise.future.recover{ case e: ArithmeticException => 0 }
       .value1.get must equalTo (0)
=======
      promise.recover{ case e: ArithmeticException => 0 }
       .value.get must equalTo (0)
>>>>>>> .merge_file_dAFw4Z
    }
  }
}
