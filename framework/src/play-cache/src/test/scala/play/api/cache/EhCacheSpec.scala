package play.api.cache

import play.api.test.{WithApplication, PlaySpecification}
import play.api.test.FakeApplication
import play.api.Play.current

class EhCacheSpec extends PlaySpecification {
  "EhCachePlugin" should {
    "load default configuration" in new WithApplication() {
      current.plugin[EhCachePlugin].get.configResource must_== current.classloader.getResource("ehcache-default.xml")
    }
    "load alternate configuration" in new WithApplication(
      FakeApplication(additionalConfiguration = Map("ehcache.configResource" -> "ehcache-alternate.xml"))
    ) {
      current.plugin[EhCachePlugin].get.configResource must_== current.classloader.getResource("ehcache-alternate.xml")
    }
  }
}
