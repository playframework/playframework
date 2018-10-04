/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.ssl

import java.io.File
import java.security.KeyStore

import sun.security.util.ObjectIdentifier

import com.typesafe.sslconfig.{ ssl => sslconfig }
import com.typesafe.sslconfig.util.NoopLogger

/**
 * A fake key store
 */
@deprecated("Deprecated in favour of the FakeKeyStore in ssl-config", "2.7.0")
object FakeKeyStore {
  private final val FakeKeyStore = new sslconfig.FakeKeyStore(NoopLogger.factory())

  val GeneratedKeyStore: String = sslconfig.FakeKeyStore.GeneratedKeyStore
  val ExportedCert: String = sslconfig.FakeKeyStore.ExportedCert
  val TrustedAlias = sslconfig.FakeKeyStore.TrustedAlias
  val DistinguishedName = sslconfig.FakeKeyStore.DistinguishedName
  val SignatureAlgorithmName = sslconfig.FakeKeyStore.SignatureAlgorithmName
  val SignatureAlgorithmOID: ObjectIdentifier = sslconfig.FakeKeyStore.SignatureAlgorithmOID

  object CertificateAuthority {
    val ExportedCertificate = sslconfig.FakeKeyStore.CertificateAuthority.ExportedCertificate
    val TrustedAlias = sslconfig.FakeKeyStore.CertificateAuthority.TrustedAlias
    val DistinguishedName = sslconfig.FakeKeyStore.CertificateAuthority.DistinguishedName
  }

  /**
   * @param appPath a file descriptor to the root folder of the project (the root, not a particular module).
   */
  def getKeyStoreFilePath(appPath: File) = FakeKeyStore.getKeyStoreFilePath(appPath)

  def createKeyStore(appPath: File): KeyStore = FakeKeyStore.createKeyStore(appPath)

}
