package sbt

import org.scalatools.testing.{ Event => TEvent, Result => TResult }

private[sbt] class PlayTestListener extends TestsListener {

  private var skipped, errors, passed, failures = 0

  private def count(event: TEvent) {
    event.result match {
      case TResult.Error => errors += 1
      case TResult.Success => passed += 1
      case TResult.Failure => failures += 1
      case TResult.Skipped => skipped += 1
    }
  }

  private def playReport(messageName: String, attributes: (String, String)*) {
    result.append("<li>" + messageName + " " + attributes.map {
      case (k, v) => k + ": " + tidy(v)
    }.mkString(" ") + "</li>")
  }

  val result = new collection.mutable.ListBuffer[String]

  override def doComplete(finalResult: TestResult.Value) {
    val totalCount = failures + errors + skipped + passed
    val postfix = "Total " + totalCount + ", Failed " + failures + ", Errors " + errors + ", Passed " + passed + ", Skipped " + skipped
    result.append("</ul>")
    result.append("<p>" + postfix + "</p>")
  }

  // There is a "side-effecting nullary methods are discouraged" warning but we can't fix it because we're overriding SBT
  override def doInit {
    result.append("<p>Executing Test suit</p>")
    result.append("<ul>")
    failures = 0
    errors = 0
    passed = 0
    skipped = 0
  }

  /** called for each class or equivalent grouping */
  override def startGroup(name: String) {
    playReport("test", "started" -> name)
  }

  /** called for each test method or equivalent */
  override def testEvent(event: TestEvent) {

    event match {
      case e =>
        for (d <- e.detail) {
          event.detail.foreach(count)
          d match {
            case te: TEvent =>
              te.result match {
                case TResult.Success => playReport("test case", "finished, result" -> event.result.get.toString)
                case TResult.Error | TResult.Failure =>
                  val e = Option(te.error).getOrElse(new RuntimeException("some unexpected error occurred during test execution"))
                  playReport("test", "failed" -> te.testName, "details" -> (e.toString +
                    "\n" + e.getStackTrace.mkString("\n at ", "\n at ", "")))
                case TResult.Skipped =>
                  playReport("test", "ignored" -> te.testName)
              }
          }
        }
    }

  }

  /** called if there was an error during test */
  override def endGroup(name: String, t: Throwable) {}
  /** called if test completed */
  override def endGroup(name: String, result: TestResult.Value) {}

  def tidy(s: String) = s
    .replace("\u0085", "")
    .replace("\u2028", "")
    .replace("\u2029", "")
}