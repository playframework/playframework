/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package play.core.server.ssl

import java.util.Properties

import org.specs2.matcher.MustThrownExpectations
import org.specs2.mutable.{ After, Specification }
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.core.ApplicationProvider
import play.core.server.ServerConfig
import scala.util.Failure
import java.io.File
import javax.net.ssl.SSLEngine
import play.server.api.SSLEngineProvider

class WrongSSLEngineProvider {}

class RightSSLEngineProvider(appPro: ApplicationProvider) extends SSLEngineProvider with Mockito {
  override def createSSLEngine: SSLEngine = {
    require(appPro != null)
    mock[SSLEngine]
  }
}

class JavaSSLEngineProvider(appPro: play.server.ApplicationProvider) extends play.server.SSLEngineProvider with Mockito {
  override def createSSLEngine: SSLEngine = {
    require(appPro != null)
    mock[SSLEngine]
  }
}

class ServerSSLEngineSpec extends Specification with Mockito {

  sequential

  trait ApplicationContext extends Mockito with Scope with MustThrownExpectations {
  }

  trait TempConfDir extends After {
    val tempDir = File.createTempFile("ServerSSLEngine", ".tmp")
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

  def serverConfig(tempDir: File, engineProvider: Option[String]) = {
    val props = new Properties()
    engineProvider.foreach(props.put("play.server.https.engineProvider", _))
    ServerConfig(rootDir = tempDir, port = Some(9000), properties = props)
  }

  def createEngine(engineProvider: Option[String], tempDir: Option[File] = None) = {
    val app = mock[ApplicationProvider]
    app.get returns Failure(new Exception("no app"))
    tempDir.foreach(dir => app.path returns dir)
    ServerSSLEngine.createSSLEngineProvider(serverConfig(tempDir.getOrElse(new File(".")), engineProvider), app)
      .createSSLEngine()
  }

  "ServerSSLContext" should {

    "default create a SSL engine suitable for development" in new ApplicationContext with TempConfDir {
      createEngine(None, Some(tempDir)) must beAnInstanceOf[SSLEngine]
    }

    "fail to load a non existing SSLEngineProvider" in new ApplicationContext {
      createEngine(Some("bla bla")) must throwA[ClassNotFoundException]
    }

    "fail to load an existing SSLEngineProvider with the wrong type" in new ApplicationContext {
      createEngine(Some(classOf[WrongSSLEngineProvider].getName)) must throwA[ClassCastException]
    }

    "load a custom SSLContext from a SSLEngineProvider" in new ApplicationContext {
      createEngine(Some(classOf[RightSSLEngineProvider].getName)) must beAnInstanceOf[SSLEngine]
    }

    "load a custom SSLContext from a java SSLEngineProvider" in new ApplicationContext {
      createEngine(Some(classOf[JavaSSLEngineProvider].getName)) must beAnInstanceOf[SSLEngine]
    }
  }

}
