/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import java.lang.reflect.Field

/**
 *
 */
trait MonkeyPatcher {

  // Define unsafe to monkeypatch fields
  private val unsafe: sun.misc.Unsafe = {
    val field = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe")
    field.setAccessible(true)
    field.get(null).asInstanceOf[sun.misc.Unsafe]
  }

  /**
   * Monkeypatches any given field.
   *
   * @param field the field to change
   * @param newObject the new object to place in the field.
   */
  def monkeyPatchField(field: Field, newObject: AnyRef) {
    val base = unsafe.staticFieldBase(field)
    val offset = unsafe.staticFieldOffset(field)
    unsafe.putObject(base, offset, newObject)
  }

}

