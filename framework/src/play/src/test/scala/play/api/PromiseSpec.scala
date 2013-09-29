/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
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
