package play.api.libs.iteratee

import org.specs2.matcher.ConcurrentExecutionContext
import scala.concurrent.ExecutionContext

/**
 * Provides an antidote to ConcurrentExecutionContext's implicit
 * ExecutionContext.
 *
 * See: https://groups.google.com/d/msg/specs2-users/m5nn4nSNu0Q/aw4sb7ha_LwJ
 */
trait NoConcurrentExecutionContext extends ConcurrentExecutionContext {
  // Override value in order to remove the implicit modifier
  override val concurrentExecutionContext = ExecutionContext.global
}
