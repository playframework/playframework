package play.api.libs.concurrent
import org.specs2.mutable._
import scala.concurrent.ExecutionContext.Implicits.global

object PromiseSpec extends Specification {

  "A Promise" should {
    "recover after an exception using recover" in {
      val promise = Promise[Int]()
      promise.redeem(6/0)
      
      promise.future.recover{ case e: ArithmeticException => 0 }
       .value1.get must equalTo (0)
    }
  }
}
object PurePromiseSpec extends Specification {
	class NonFatalThrowable extends Throwable

	"A pure promise" should {
		"recover after a NonFatal error" in {
			val promise = PurePromise[String](throw new NonFatalThrowable)
			val recovered=promise.recover{ case e: NonFatalThrowable => "NonFatalThrowable"}
			recovered.value1.get must equalTo ("NonFatalThrowable")
		}
	}	
}