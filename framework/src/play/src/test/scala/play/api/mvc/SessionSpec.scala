/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.mvc

import org.specs2.mutable._

class SessionSpec extends Specification {

  "Session" should {

    "be able to create an empty Session" in {
      val session = Session()
      session.isEmpty mustEqual true
    }

    "store a key-value pair of strings" in {
      val session = Session(Map("key" -> "value"))
      (session.isEmpty mustEqual false) and (session.get("key") mustEqual Some("value"))
    }

    "store multiple key-value pairs of strings" in {
      val session = Session(
        Map(
          "key" -> "value",
          "anOtherKey" -> "anOtherValue"
        ))
      (session.isEmpty mustEqual false) and
        (session.get("key") mustEqual Some("value")) and
        (session.get("anOtherKey") mustEqual Some("anOtherValue"))
    }

    "throw an illegalArgumentException when initialized with data containing a null value" in {
      Session(Map("key" -> null)) must throwA[IllegalArgumentException]
    }

    "throw an illegalArgumentException when initialized with data containing a null key" in {
      Session(Map((null: String) -> "value")) must throwA[IllegalArgumentException]
    }

    "throw an illegalArgumentException when initialized with data containing a null key and a null value" in {
      Session(
        Map(
          "key" -> "value",
          (null: String) -> null
        )) must throwA[IllegalArgumentException]
    }

    "return an other session containing the added key-value pair" in {
      val emptySession = Session()
      val sessionWtihKeyValue = emptySession + ("key" -> "value")
      (emptySession.isEmpty mustEqual true) and
        (sessionWtihKeyValue.isEmpty mustEqual false) and
        (sessionWtihKeyValue("key") mustEqual "value")
    }

    "throw an illegalArgumentException when adding a null value" in {
      val session = Session(Map("key1" -> "value1"))
      session + ("key2" -> null) must throwA[IllegalArgumentException]
    }

    "throw an illegalArgumentException when adding a null key" in {
      val session = Session(Map("key1" -> "value1"))
      session + ((null: String) -> "value") must throwA[IllegalArgumentException]
    }

    "throw an illegalArgumentException when adding a null key and a null value" in {
      val session = Session(Map("key1" -> "value1"))
      session + ((null: String) -> null) must throwA[IllegalArgumentException]
    }

    "return a new session without the removed key-value pair" in {
      val sessionWithKeyValue = Session(Map("key" -> "value"))
      val sessionWithoutKeyValue = sessionWithKeyValue - "key"
      (sessionWithKeyValue.isEmpty mustEqual false) and (sessionWithoutKeyValue.isEmpty mustEqual true)
    }

    "throws a NoSuchElementException when getting an unknown key" in {
      val emptySession = Session()
      emptySession("key") must throwA[NoSuchElementException]
    }

  }

}
