/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
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