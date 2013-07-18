package play.core.j

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.SECONDS
import org.specs2.mutable.Specification
import play.api.libs.iteratee.ExecutionSpecification
import play.mvc.Http
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

object HttpExecutionContextSpec extends Specification
  with ExecutionSpecification {

  "HttpExecutionContext" should {

    "propagate the context ClassLoader and Http.Context" in {
      mustExecute(2) { ec =>
        val pec = ec.prepare()
        val classLoader = new ClassLoader() {}
        val httpContext = new Http.Context(1, null, null, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava)
        val hec = new HttpExecutionContext(classLoader, httpContext, pec)

        val hecFromThread = new LinkedBlockingQueue[ExecutionContext]()
        hec.execute(new Runnable {
          def run() = {
            hecFromThread.offer(HttpExecutionContext.fromThread(pec))
          }
        })

        val actualClassLoader = new LinkedBlockingQueue[ClassLoader]()
        val actualHttpContext = new LinkedBlockingQueue[Http.Context]()
        hecFromThread.poll(5, SECONDS).execute(new Runnable {
          def run() = {
            actualClassLoader.offer(Thread.currentThread().getContextClassLoader())
            actualHttpContext.offer(Http.Context.current.get())
          }
        })
        actualClassLoader.poll(5, SECONDS) must equalTo(classLoader)
        actualHttpContext.poll(5, SECONDS) must equalTo(httpContext)
      }
    }

  }

}
