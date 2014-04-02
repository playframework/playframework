package play.core.server


import java.io.File
import javax.net.ssl.SSLContext
import org.specs2.mock.Mockito
import org.specs2.mutable.{ After, Specification }
import org.specs2.specification.Scope
import play.core.ApplicationProvider
import play.server
import play.server.api.SSLContextProvider
import scala.util.Failure

class WrongSSLContextProvider {}

class RightSSLContextProvider extends SSLContextProvider with Mockito {
  override def createSSLContext(appPro: ApplicationProvider): SSLContext = mock[SSLContext]
}

class JavaSSLContextProvider extends play.server.SSLContextProvider with Mockito {
  override def createSSLContext(applicationProvider: server.ApplicationProvider): SSLContext = mock[SSLContext]
}

object ServerSSLContextSpec extends Specification with Mockito {

  sequential

  import ServerSSLContext.loadSSLContext

  trait ApplicationContext extends Mockito with Scope {
    val applicationProvider = mock[ApplicationProvider]
    applicationProvider.get returns Failure(new Exception("no app"))
  }

  trait TempConfDir extends After {
    val tempDir = File.createTempFile("ServerSSLContext", ".tmp")
    tempDir.delete()
    val confDir = new File(tempDir, "conf")
    confDir.mkdirs()

    def after = {
      confDir.listFiles().foreach(f => f.delete())
      tempDir.listFiles().foreach(f => f.delete())
      tempDir.delete()
    }
  }


  val javaAppProvider = mock[play.core.ApplicationProvider]

  "ServerSSLContext" should {

    "default create a SSL context suitable for development" in new ApplicationContext with TempConfDir {
      applicationProvider.path returns tempDir
      System.clearProperty("play.http.sslcontextprovider")
      loadSSLContext(applicationProvider) should beAnInstanceOf[SSLContext]
    }

    "fail to load a non existing SSLContextProvider" in new ApplicationContext {
      System.setProperty("play.http.sslcontextprovider", "bla bla")
      loadSSLContext(applicationProvider) should throwA[ClassNotFoundException]
    }

    "fail to load an existing SSLContextProvider with the wrong type" in new ApplicationContext {
      System.setProperty("play.http.sslcontextprovider", classOf[WrongSSLContextProvider].getName)
      loadSSLContext(applicationProvider) should throwA[ClassCastException]
    }

    "load a custom SSLContext from a SSLContextProvider" in new ApplicationContext {
      System.setProperty("play.http.sslcontextprovider", classOf[RightSSLContextProvider].getName)
      loadSSLContext(applicationProvider) should beAnInstanceOf[SSLContext]
    }

    "load a custom SSLContext from a java SSLContextProvider" in new ApplicationContext {
      System.setProperty("play.http.sslcontextprovider", classOf[JavaSSLContextProvider].getName)
      loadSSLContext(applicationProvider) should beAnInstanceOf[SSLContext]
    }
  }
}
