/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
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
@deprecated("Deprecated in favour of the com.typesafe.sslconfig.ssl.FakeKeyStore in ssl-config", "2.7.0")
object FakeKeyStore {
  private final val FakeKeyStore = new sslconfig.FakeKeyStore(NoopLogger.factory())

  val GeneratedKeyStore: String = sslconfig.FakeKeyStore.KeystoreSettings.GeneratedKeyStore
  val TrustedAlias: String = sslconfig.FakeKeyStore.SelfSigned.Alias.trustedCertEntry
  val DistinguishedName: String = sslconfig.FakeKeyStore.SelfSigned.DistinguishedName
  val SignatureAlgorithmName: String = sslconfig.FakeKeyStore.KeystoreSettings.SignatureAlgorithmName
  val SignatureAlgorithmOID: ObjectIdentifier = sslconfig.FakeKeyStore.KeystoreSettings.SignatureAlgorithmOID

  /**
   * @param appPath a file descriptor to the root folder of the project (the root, not a particular module).
   */
  def getKeyStoreFilePath(appPath: File): File = FakeKeyStore.getKeyStoreFilePath(appPath)

  def createKeyStore(appPath: File): KeyStore = FakeKeyStore.createKeyStore(appPath)

}
