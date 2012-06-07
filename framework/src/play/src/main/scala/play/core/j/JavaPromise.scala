package play.core.j

import java.util.{ List => JList }
import play.api.libs.concurrent._
import scala.collection.JavaConverters
import play.libs.F
import java.util.concurrent.TimeoutException

object JavaPromise {

  def sequence[A](promises: JList[F.Promise[_ <: A]]): Promise[JList[A]] = {
    Promise.sequence(JavaConverters.asScalaBufferConverter(promises).asScala.map(_.getWrappedPromise))
      .map(az => JavaConverters.bufferAsJavaListConverter(az).asJava)
  }

  def timeout[A](message: A, delay: Long, unit:java.util.concurrent.TimeUnit) = {
    Promise.timeout(message, delay, unit)
  }
  
  def timeout: Promise[TimeoutException] = Promise.timeout

  def recover[A](promise: Promise[A], f: Throwable => Promise[A]): Promise[A] = {
    promise.extend1 {
      case Thrown(e) => f(e)
      case Redeemed(a) => Promise.pure(a)
    }.flatMap( p => p )
  }

  def pure[A](a: A) = Promise.pure(a)

  def throwing[A](throwable: Throwable) = Promise.pure[A](throw throwable)

}
