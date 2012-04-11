package play.core.j

import java.util.{ List => JList }
import play.api.libs.concurrent.{ Promise }
import scala.collection.JavaConverters
import play.libs.F

object JavaPromise {

  def sequence[A](promises: JList[F.Promise[A]]): Promise[JList[A]] =
    Promise.sequence(JavaConverters.asScalaBufferConverter(promises).asScala.map(_.getWrappedPromise))
      .map(az => JavaConverters.bufferAsJavaListConverter(az).asJava)

}
