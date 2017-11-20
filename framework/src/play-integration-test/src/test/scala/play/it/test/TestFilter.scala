package play.it.test

import org.specs2.execute._

/**
 * Controls whether a test is run, skipped or pending. Used to
 * standardize the way we filter tests when running endpoints,
 * since we don't have direct access to the usual testing DSL.
 */
sealed trait TestFilter {
  def executeFiltered[A: AsResult](resultBlock: => A): Result
}
object TestFilter {

  /** Indicates a test should be run. */
  case object Run extends TestFilter {
    override def executeFiltered[A: AsResult](resultBlock: => A): Result =
      ResultExecution.execute(AsResult(resultBlock))
  }

  /** Indicates a test is failing and a fix is pending. */
  case object Pending extends TestFilter {
    override def executeFiltered[A: AsResult](resultBlock: => A): Result =
      PendingUntilFixed.toPendingUntilFixed(resultBlock).pendingUntilFixed
  }

  /** Indicates a test is skipped and not run at all. */
  case object Skipped extends TestFilter {
    override def executeFiltered[A: AsResult](resultBlock: => A): Result =
      StandardResults.skipped
  }

}
