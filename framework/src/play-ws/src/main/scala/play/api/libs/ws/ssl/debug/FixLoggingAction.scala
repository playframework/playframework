/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl.debug

import java.lang.reflect.Field
import java.security.PrivilegedExceptionAction
import play.api.libs.ws.ssl.MonkeyPatcher

/**
 * A privileged action that will find relevant classes containing static final fields of type T and replace
 * them with the object referenced by {{newDebug}}, and switch out the "args" field value with the value defined
 * in {{newOptions}}.  This is the only way to change JSSE debugging after the class loads.
 */
abstract class FixLoggingAction extends PrivilegedExceptionAction[Unit] with MonkeyPatcher with ClassFinder {

  def newOptions: String

  def isValidField(field: Field, definedType: Class[_]): Boolean = {
    import java.lang.reflect.Modifier._
    val modifiers: Int = field.getModifiers
    field.getType == definedType && isStatic(modifiers)
  }

}
