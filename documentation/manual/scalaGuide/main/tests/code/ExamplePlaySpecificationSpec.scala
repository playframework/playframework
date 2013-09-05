package scalaguide.tests

import play.api.test._

// #scalatest-playspecification
object ExamplePlaySpecificationSpec extends PlaySpecification {
  "The specification" should {

    "have access to HeaderNames" in {
      USER_AGENT must be_===("User-Agent")
    }

    "have access to Status" in {
      OK must be_===(200)
    }
  }
}
// #scalatest-playspecification
