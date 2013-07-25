package play.api

import org.specs2.mutable.Specification

object ConfigurationSpec extends Specification {

  def exampleConfig = Configuration.from(
    Map(
      "foo.bar1" -> "value1",
      "foo.bar2" -> "value2",
      "blah" -> List("value3", "value4", "value5"),
      "blah2" -> Map(
        "blah3" -> Map(
          "blah4" -> "value6"
        )
      )
    )
  )

  "Configuration" should {

    "be accessible as an entry set" in {
      val map = Map(exampleConfig.entrySet.toList:_*)
      map.keySet must contain(exactly("foo.bar1", "foo.bar2", "blah", "blah2.blah3.blah4"))
    }

    "make all paths accessible" in {
      exampleConfig.keys must contain(exactly("foo.bar1", "foo.bar2", "blah", "blah2.blah3.blah4"))
    }

    "make all sub keys accessible" in {
      exampleConfig.subKeys must contain(exactly("foo", "blah", "blah2"))
    }

  }

}
