/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.api.libs.ws.ssl

/**
 * @see http://docs.oracle.com/javase/6/docs/technotes/guides/security/certpath/CertPathProgGuide.html
 */
class JavaSecurityDebugBuilder(c: SSLDebugConfig) {

  def build(): String = {
    val b = new StringBuilder()
    if (c.certpath) {
      b.append(" certpath")
    }

    if (c.ocsp) {
      // http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/7u40-b43/sun/security/provider/certpath/OCSPResponse.java#132
      b.append(" ocsp")
    }

    b.toString()
  }

}

/**
 * A builder for setting the system property options in "javax.net.debug" and in "java.security.debug' (in
 * the case of "certpath").
 *
 * @see http://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html#Debug
 * @see http://docs.oracle.com/javase/6/docs/technotes/guides/security/jsse/ReadDebug.html
 */
class JavaxNetDebugBuilder(c: SSLDebugConfig) {

  def build(): String = {
    if (c.all) return "all"
    buildSSL(c)
  }

  protected def buildSSL(sslDebugConfig: SSLDebugConfig): String = {
    import sslDebugConfig._

    val b = new StringBuilder()

    record.map {
      r =>
        b.append(" record")
        if (r.packet) {
          b.append(" packet")
        }
        if (r.plaintext) {
          b.append(" plaintext")
        }
    }

    handshake.map {
      h =>
        b.append(" handshake")
        if (h.data) {
          b.append(" data")
        }
        if (h.verbose) {
          b.append(" verbose")
        }
    }

    if (keygen) {
      b.append(" keygen")
    }

    if (session) {
      b.append(" session")
    }

    if (defaultctx) {
      b.append(" defaultctx")
    }

    if (sslctx) {
      b.append(" sslctx")
    }

    if (sessioncache) {
      b.append(" sessioncache")
    }

    if (keymanager) {
      b.append(" keymanager")
    }

    if (trustmanager) {
      b.append(" trustmanager")
    }

    if (pluggability) {
      b.append(" pluggability")
    }

    if ((!b.isEmpty) || ssl) {
      b.append(" ssl")
    }

    b.toString()
  }
}
