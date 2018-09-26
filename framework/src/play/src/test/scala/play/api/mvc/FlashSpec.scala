/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import org.specs2.mutable._

class FlashSpec extends Specification {

  "Flash" should {

    "be able to create an empty Flash" in {
      val flash = Flash()
      flash.isEmpty mustEqual true
    }

    "store a key-value pair of strings" in {
      val flash = Flash(Map("key" -> "value"))
      (flash.isEmpty mustEqual false) and (flash.get("key") mustEqual Some("value"))
    }

    "store multiple key-value pairs of strings" in {
      val flash = Flash(
        Map(
          "key" -> "value",
          "anOtherKey" -> "anOtherValue"
        ))
      (flash.isEmpty mustEqual false) and
        (flash.get("key") mustEqual Some("value")) and
        (flash.get("anOtherKey") mustEqual Some("anOtherValue"))
    }

    "throw an illegalArgumentException when initialized with data containing a null value" in {
      Flash(Map("key" -> null)) must throwA[IllegalArgumentException]
    }

    "throw an illegalArgumentException when initialized with data containing a null key" in {
      Flash(Map((null: String) -> "value")) must throwA[IllegalArgumentException]
    }

    "throw an illegalArgumentException when initialized with data containing a null key and a null value" in {
      Flash(
        Map(
          "key" -> "value",
          (null: String) -> null
        )) must throwA[IllegalArgumentException]
    }

    "return an other flash containing the added key-value pair" in {
      val emptyFlash = Flash()
      val flashWtihKeyValue = emptyFlash + ("key" -> "value")
      (emptyFlash.isEmpty mustEqual true) and
        (flashWtihKeyValue.isEmpty mustEqual false) and
        (flashWtihKeyValue("key") mustEqual "value")
    }

    "throw an illegalArgumentException when adding a null value" in {
      val flash = Flash(Map("key1" -> "value1"))
      flash + ("key2" -> null) must throwA[IllegalArgumentException]
    }

    "throw an illegalArgumentException when adding a null key" in {
      val flash = Flash(Map("key1" -> "value1"))
      flash + ((null: String) -> "value") must throwA[IllegalArgumentException]
    }

    "throw an illegalArgumentException when adding a null key and a null value" in {
      val flash = Flash(Map("key1" -> "value1"))
      flash + ((null: String) -> null) must throwA[IllegalArgumentException]
    }

    "return a new flash without the removed key-value pair" in {
      val flashWithKeyValue = Flash(Map("key" -> "value"))
      val flashWithoutKeyValue = flashWithKeyValue - "key"
      (flashWithKeyValue.isEmpty mustEqual false) and (flashWithoutKeyValue.isEmpty mustEqual true)
    }

    "throws a NoSuchElementException when getting an unknown key" in {
      val emptyFlash = Flash()
      emptyFlash("key") must throwA[NoSuchElementException]
    }

  }

}
