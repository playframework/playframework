package scala

// #scalaexample
import javax.net.ssl.SSLContext
import play.core.ApplicationProvider
import play.server.api.SSLContextProvider

class CustomSSLContextProvider extends SSLContextProvider {

  override def createSSLContext(appProvider: ApplicationProvider): SSLContext = {
    // change it to your custom implementation
    SSLContext.getDefault
  }

}
// #scalaexample
