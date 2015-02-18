/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

import java.security.cert.CertificateException

/**
 * A certificate exception that contains underlying exceptions.
 */
class CompositeCertificateException(msg: String, val throwables: Array[Throwable]) extends CertificateException(msg) {
  def getSourceExceptions: Array[Throwable] = throwables
}

object CompositeCertificateException {

  def unwrap(e: Throwable)(block: Throwable => Unit) = {
    var cause: Throwable = e
    while (cause != null) {
      cause match {
        case composite: CompositeCertificateException =>
          composite.getSourceExceptions.foreach { sourceException =>
            block(sourceException)
          }
        case other =>
          block(other)
      }
      cause = cause.getCause
    }
  }

}
