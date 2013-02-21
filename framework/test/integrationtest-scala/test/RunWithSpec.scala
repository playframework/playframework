package test
import org.specs2.mutable._

import org.junit.runner._
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope
import play.api.libs.Files._

@RunWith(classOf[JUnitRunner])
class RunWithSpec extends Specification {

  class ScopeVar extends Scope {
    RunWithSpecVar.count += 1
  }

  "Specs with runWith" should {
      "execute once" in new ScopeVar {
        RunWithSpecVar.count === 1
      }
  }
}

object RunWithSpecVar {
  var count = 0
}
  
