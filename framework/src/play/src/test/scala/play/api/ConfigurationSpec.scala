/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import org.specs2.mutable.Specification

object ConfigurationSpec extends Specification {

  def exampleConfig = Configuration.from(
    Map(
      "foo.bar1" -> "value1",
      "foo.bar2" -> "value2",
      "blah.0" -> List(true, false, true),
      "blah.1" -> List(1, 2, 3),
      "blah.2" -> List(1.1, 2.2, 3.3),
      "blah.3" -> List(1L, 2L, 3L),
      "blah.4" -> List("one", "two", "three"),
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
      map.keySet must contain(exactly("foo.bar1", "foo.bar2", "blah.\"0\"", "blah.\"1\"", "blah.\"2\"", "blah.\"3\"", "blah.\"4\"", "blah2.blah3.blah4"))
    }

    "make all paths accessible" in {
      exampleConfig.keys must contain(exactly("foo.bar1", "foo.bar2", "blah.\"0\"", "blah.\"1\"", "blah.\"2\"", "blah.\"3\"", "blah.\"4\"", "blah2.blah3.blah4"))
    }

    "make all sub keys accessible" in {
      exampleConfig.subKeys must contain(exactly("foo", "blah", "blah2"))
    }

    "make all get accessible using scala" in {
      exampleConfig.getBooleanSeq("blah.0").get must ===(Seq(true, false, true))
      exampleConfig.getIntSeq("blah.1").get     must ===(Seq(1, 2, 3))
      exampleConfig.getDoubleSeq("blah.2").get  must ===(Seq(1.1, 2.2, 3.3))
      exampleConfig.getLongSeq("blah.3").get    must ===(Seq(1L, 2L, 3L))
      exampleConfig.getStringSeq("blah.4").get  must contain(exactly("one", "two", "three"))
    }

  }

}
