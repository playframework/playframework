package play.api.test

import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import play.api.http.{ HttpProtocol, Status, HeaderNames }

/**
 * Play specs2 specification.
 *
 * This trait excludes some of the mixins provided in the default specs2 specification that clash with Play helpers
 * methods.  It also mixes in the Play test helpers and types for convenience.
 */
trait PlaySpecification extends Specification
    with NoTimeConversions
    with PlayRunners
    with HeaderNames
    with Status
    with HttpProtocol
    with DefaultAwaitTimeout
    with ResultExtractors
    with Writeables
    with RouteInvokers
    with WsTestClient
    with FutureAwaits {
}