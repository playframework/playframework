package sbt

import org.scalatools.testing.{ Event => TEvent, Result => TResult }

private[sbt] class PlayTestListener extends TestReportListener {

  val result = new collection.mutable.ListBuffer[String]

  /** called for each class or equivalent grouping */
  def startGroup(name: String) {
    playReport("test", "started" -> name)
  }

  /** called for each test method or equivalent */
  def testEvent(event: TestEvent) {

    event match {
      case e =>
        for (d <- e.detail) {
          d match {
            case te: TEvent =>
              te.result match {
                case TResult.Success => playReport("test case", "finished, result" -> event.result.get.toString)
                case TResult.Error | TResult.Failure =>
                  playReport("test", "failed" -> te.testName, "details" -> (te.error.toString +
                    "\n" + te.error.getStackTrace.mkString("\n at ", "\n at ", "")))
                case TResult.Skipped =>
                  playReport("test", "ignored" -> te.testName)
              }
          }
        }
    }

  }

  /** called if there was an error during test */
  def endGroup(name: String, t: Throwable) {}
  /** called if test completed */
  def endGroup(name: String, result: TestResult.Value) {}

  def tidy(s: String) = s
    .replace("|", "||")
    .replace("'", "|'")
    .replace("\n", "|n")
    .replace("\r", "|r")
    .replace("\u0085", "|x")
    .replace("\u2028", "|l")
    .replace("\u2029", "|p")
    .replace("[", "|[")
    .replace("]", "|]")

  private def playReport(messageName: String, attributes: (String, String)*) {
    result.append("<li>" + messageName + " " + attributes.map {
      case (k, v) => k + ": " + tidy(v)
    }.mkString(" ") + "</li>")
  }
}
