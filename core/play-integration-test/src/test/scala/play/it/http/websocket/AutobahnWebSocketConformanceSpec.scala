/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.websocket

import org.specs2.mutable.Specification
import play.api.libs.json.Json

class AutobahnWebSocketConformanceSpec extends Specification {

  "AutobahnWebSocketConformance" should {
    "accept successful, non-strict, and informational case behavior" in {
      val report = Json.obj(
        "Play-netty" -> Json.obj(
          "1.1.1"  -> result("OK", "OK"),
          "1.1.2"  -> result("NON-STRICT", "OK"),
          "7.13.1" -> result("INFORMATIONAL", "INFORMATIONAL")
        )
      )

      val evaluation = AutobahnWebSocketConformance.evaluateReport(report)

      evaluation.results must haveSize(3)
      evaluation.failures must beEmpty
      evaluation.behaviorCounts must_== Map("OK" -> 1, "NON-STRICT" -> 1, "INFORMATIONAL" -> 1)
    }

    "report failed protocol and closing behavior" in {
      val report = Json.obj(
        "Play-pekko-http" -> Json.obj(
          "3.1"   -> result("FAILED", "OK"),
          "7.1.1" -> result("OK", "UNCLEAN")
        )
      )

      val evaluation = AutobahnWebSocketConformance.evaluateReport(report)

      evaluation.failures.map(result => result.caseId -> (result.behavior, result.closeBehavior)) must contain(
        exactly(
          "3.1"   -> ("FAILED", "OK"),
          "7.1.1" -> ("OK", "UNCLEAN")
        )
      )
    }

    "accept only explicitly expected unimplemented cases" in {
      val report = Json.obj(
        "Play-pekko-http" -> Json.obj(
          "13.3.1" -> result("UNIMPLEMENTED", "OK"),
          "13.6.1" -> result("UNIMPLEMENTED", "OK")
        )
      )

      val evaluation = AutobahnWebSocketConformance.evaluateReport(report, Seq("13.3."))

      evaluation.failures.map(_.caseId) must contain(exactly("13.6.1"))
    }
  }

  private def result(behavior: String, closeBehavior: String) =
    Json.obj(
      "behavior"      -> behavior,
      "behaviorClose" -> closeBehavior,
      "reportfile"    -> "case.json"
    )
}
