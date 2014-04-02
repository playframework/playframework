package play.core.server

import java.io.{ FileInputStream, File }
import java.security.KeyStore
import javax.net.ssl.{ TrustManager, KeyManagerFactory, SSLContext }
import play.api.{ Play, PlayException }
import play.core.ApplicationProvider
import play.core.server.netty.FakeKeyStore
import play.server.api.SSLContextProvider
import scala.util.control.NonFatal
import scala.util.{ Success, Failure, Try }

object ServerSSLContext {

  def loadSSLContext(applicationProvider: ApplicationProvider): SSLContext = {
    val providerClass = Option(System.getProperty("play.http.sslcontextprovider")).getOrElse(classOf[DefaultSSLContextProvider].getName)
    val classLoader = applicationProvider.get.map(_.classloader).getOrElse(this.getClass.getClassLoader)
    try {

      val providerInstance = classLoader.loadClass(providerClass).getConstructor().newInstance().asInstanceOf[SSLContextProvider]
      providerInstance.createSSLContext(applicationProvider)

    } catch {
      case e: ClassCastException => {
        // try the Java interface
        val providerInstance = classLoader.loadClass(providerClass).getConstructor().newInstance().asInstanceOf[play.server.SSLContextProvider]
        val javaApplication = applicationProvider.get.map(a => new play.Application(a)).getOrElse(null)
        val javaAppProvider = new play.server.ApplicationProvider(javaApplication, applicationProvider.path)
        providerInstance.createSSLContext(javaAppProvider)
      }
    }
  }

}
