/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.ssl

import java.io.File
import java.nio.file.Files
import java.util.Properties
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine

import org.mockito.Mockito
import org.specs2.matcher.MustThrownExpectations
import org.specs2.mutable.After
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.core.server.ServerConfig
import play.core.ApplicationProvider
import play.server.api.SSLEngineProvider

class WrongSSLEngineProvider {}

class RightSSLEngineProvider(appPro: ApplicationProvider) extends SSLEngineProvider {
  override def createSSLEngine: SSLEngine = {
    require(appPro != null)
    Mockito.mock(classOf[SSLEngine])
  }

  override def sslContext(): SSLContext = {
    require(appPro != null)
    Mockito.mock(classOf[SSLContext])
  }
}

class JavaSSLEngineProvider(appPro: play.server.ApplicationProvider) extends play.server.SSLEngineProvider {
  override def createSSLEngine: SSLEngine = {
    require(appPro != null)
    Mockito.mock(classOf[SSLEngine])
  }

  override def sslContext(): SSLContext = {
    require(appPro != null)
    Mockito.mock(classOf[SSLContext])
  }
}

class ServerSSLEngineSpec extends Specification {
  sequential

  trait ApplicationContext extends Mockito with Scope with MustThrownExpectations {}

  trait TempConfDir extends After {
    val tempDir: File = Files.createTempFile("ServerSSLEngine", ".tmp").toFile
    tempDir.delete()
    val confDir = new File(tempDir, "conf")
    confDir.mkdirs()

    override def after: Boolean = {
      confDir.listFiles().foreach(f => f.delete())
      tempDir.listFiles().foreach(f => f.delete())
      tempDir.delete()
    }
  }

  def serverConfig(tempDir: File, engineProvider: Option[String]): ServerConfig = {
    val props = new Properties()
    engineProvider.foreach(props.put("play.server.https.engineProvider", _))
    ServerConfig(rootDir = tempDir, port = Some(9000), properties = props)
  }

  def createEngine(engineProvider: Option[String], tempDir: Option[File] = None): SSLEngine = {
    val app = Mockito.mock(classOf[play.api.Application])
    Mockito.when(app.classloader).thenReturn(this.getClass.getClassLoader)
    Mockito.when(app.asJava).thenReturn(Mockito.mock(classOf[play.Application]))

    val appProvider = Mockito.mock(classOf[ApplicationProvider])
    Mockito.when(appProvider.get).thenReturn(scala.util.Success(app)) // Failure(new Exception("no app"))
    ServerSSLEngine
      .createSSLEngineProvider(serverConfig(tempDir.getOrElse(new File(".")), engineProvider), appProvider)
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
