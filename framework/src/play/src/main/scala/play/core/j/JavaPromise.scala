package play.core.j

import java.util.{List => JList}
import play.api.libs.concurrent.{Promise}
import scala.collection.JavaConverters._
import play.libs.F

object JavaPromise {

    def sequence[A](promises:JList[F.Promise[A]]):Promise[JList[A]] = 
      Promise.sequence(promises.asScala.map(_.getWrappedPromise))
             .map(az => az.asJava)

}
