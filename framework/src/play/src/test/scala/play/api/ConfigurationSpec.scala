package play.api

import org.specs2.mutable.Specification

object ConfigurationSpec extends Specification {

  def exampleConfig = Configuration.from(Map("foo.bar1" -> "value1", "foo.bar2" -> "value2", "blah" -> "value3"))

  "Configuration" should {

    "be accessible as an entry set" in {
      val map = Map(exampleConfig.entrySet.toList:_*)
      map.keySet must contain("foo.bar1", "foo.bar2", "blah").only
    }

    "make all paths accessible" in {
      exampleConfig.keys must contain("foo.bar1", "foo.bar2", "blah").only
    }

    "make all sub keys accessible" in {
      exampleConfig.subKeys must contain("foo", "blah").only
    }

  }

}
