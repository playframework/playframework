/*
 *
 *  * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl.debug

import play.api.libs.ws.ssl._

import java.security.AccessController
import scala.util.control.NonFatal

/**
 * This is provided to fix logging (for either
 */
object FixInternalDebugLogging {

  class MonkeyPatchInternalSslDebugAction(val newDebug:AnyRef, val newOptions:String) extends FixLoggingAction {

    def initialResource = "/javax/net/ssl/SSLContext.class"

    /**
     * Returns true if this class has an instance of {{Debug.getInstance("certpath")}}, false otherwise.
     *
     * @param className the name of the class.
     * @return true if this class should be returned in the set of findClasses, false otherwise.
     */
    def isValidClass(className: String): Boolean = {
      className.startsWith("com.sun.net.ssl.internal.ssl") || className.startsWith("sun.security.ssl")
    }

    def run() {
      val debugType = newDebug.getClass

      for (debugClass <- findClasses) {
        for (debugField <- debugClass.getDeclaredFields) {
          if (isValidField(debugField, debugType)) {
            Console.println(s"Patching field ${debugField} in class $debugClass")

            monkeyPatchField(debugField, newDebug)
          }
        }
      }

      // Switch out the args (for certpath loggers that AREN'T static and final)
      // This will result in those classes using the base Debug class which will write to System.out, but
      // I don't know how to switch out the Debug.getInstance method itself without using a java agent.
      val argsField = debugType.getDeclaredField("args")
      monkeyPatchField(argsField, newOptions)
    }
  }

  def classType : Class[_] = {
    val debugClassName = foldVersion(
      run16 = "com.sun.net.ssl.internal.ssl.Debug",
      runHigher = "sun.security.ssl.Debug"
    )
    Thread.currentThread().getContextClassLoader.loadClass(debugClassName)
  }

  def apply(newOptions:String, debugOption: Option[AnyRef] = None) {
    try {
      val newDebug : AnyRef = debugOption match {
        case Some(debug) => debug
        case None => classType.newInstance().asInstanceOf[AnyRef]
      }

      val action = new MonkeyPatchInternalSslDebugAction(newDebug, newOptions)
      AccessController.doPrivileged(action)
    } catch {
      case NonFatal(e) =>
        throw new IllegalStateException("FixInternalDebugLogging configuration error", e)
    }
  }
}
