/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.server.ssl

import play.core.server.ServerConfig
import play.server.api.{ SSLEngineProvider => ScalaSSLEngineProvider }
import play.server.{ SSLEngineProvider => JavaSSLEngineProvider }
import java.lang.reflect.Constructor
import play.core.ApplicationProvider

/**
 * This singleton object looks for a class of {{play.server.api.SSLEngineProvider}} or {{play.server.SSLEngineProvider}}
 * in the system property <pre>play.http.sslengineprovider</pre>.  if there is no instance found, it uses
 * DefaultSSLEngineProvider.
 *
 * If the class of {{SSLEngineProvider}} defined has a constructor with {{play.core.ApplicationProvider}} (for Scala) or
 * {{play.server.ApplicationProvider}} (for Java), then an application provider is passed in when a new instance of the
 * class is created.
 */
object ServerSSLEngine {

  def createSSLEngineProvider(serverConfig: ServerConfig, applicationProvider: ApplicationProvider): JavaSSLEngineProvider = {
    val providerClassName = serverConfig.configuration.underlying.getString("play.server.https.engineProvider")

    val classLoader = applicationProvider.get.map(_.classloader).getOrElse(this.getClass.getClassLoader)
    val providerClass = classLoader.loadClass(providerClassName)

    // NOTE: this is not like instanceof.  With isAssignableFrom, the subclass should be on the right.
    providerClass match {
      case i if classOf[ScalaSSLEngineProvider].isAssignableFrom(providerClass) =>
        createScalaSSLEngineProvider(i.asInstanceOf[Class[ScalaSSLEngineProvider]], serverConfig, applicationProvider)

      case s if classOf[JavaSSLEngineProvider].isAssignableFrom(providerClass) =>
        createJavaSSLEngineProvider(s.asInstanceOf[Class[JavaSSLEngineProvider]], serverConfig, applicationProvider)

      case _ =>
        throw new ClassCastException("Must define play.server.api.SSLEngineProvider or play.server.SSLEngineProvider as interface!")
    }
  }

  private def createJavaSSLEngineProvider(providerClass: Class[JavaSSLEngineProvider],
    serverConfig: ServerConfig, applicationProvider: ApplicationProvider): JavaSSLEngineProvider = {
    var serverConfigProviderArgsConstructor: Constructor[_] = null
    var providerArgsConstructor: Constructor[_] = null
    var noArgsConstructor: Constructor[_] = null
    for (constructor <- providerClass.getConstructors) {
      val parameterTypes = constructor.getParameterTypes
      if (parameterTypes.length == 0) {
        noArgsConstructor = constructor
      } else if (parameterTypes.length == 1 && classOf[play.server.ApplicationProvider].isAssignableFrom(parameterTypes(0))) {
        providerArgsConstructor = constructor
      } else if (parameterTypes.length == 2 &&
        classOf[ServerConfig].isAssignableFrom(parameterTypes(0)) &&
        classOf[play.server.ApplicationProvider].isAssignableFrom(parameterTypes(1))) {
        serverConfigProviderArgsConstructor = constructor
      }
    }

    def javaAppProvider = {
      val javaApplication = applicationProvider.get.map(a => a.injector.instanceOf[play.Application]).getOrElse(null)
      new play.server.ApplicationProvider(javaApplication, applicationProvider.path)
    }

    if (serverConfigProviderArgsConstructor != null) {
      serverConfigProviderArgsConstructor.newInstance(serverConfig, javaAppProvider).asInstanceOf[JavaSSLEngineProvider]
    } else if (providerArgsConstructor != null) {
      providerArgsConstructor.newInstance(javaAppProvider).asInstanceOf[JavaSSLEngineProvider]
    } else if (noArgsConstructor != null) {
      noArgsConstructor.newInstance().asInstanceOf[play.server.SSLEngineProvider]
    } else {
      throw new ClassCastException("No constructor with (appProvider:play.server.ApplicationProvider) or no-args constructor defined!")
    }
  }

  private def createScalaSSLEngineProvider(providerClass: Class[ScalaSSLEngineProvider],
    serverConfig: ServerConfig, applicationProvider: ApplicationProvider): ScalaSSLEngineProvider = {

    var serverConfigProviderArgsConstructor: Constructor[ScalaSSLEngineProvider] = null
    var providerArgsConstructor: Constructor[ScalaSSLEngineProvider] = null
    var noArgsConstructor: Constructor[ScalaSSLEngineProvider] = null
    for (constructor <- providerClass.getConstructors) {
      val parameterTypes = constructor.getParameterTypes
      if (parameterTypes.length == 0) {
        noArgsConstructor = constructor.asInstanceOf[Constructor[ScalaSSLEngineProvider]]
      } else if (parameterTypes.length == 1 && classOf[ApplicationProvider].isAssignableFrom(parameterTypes(0))) {
        providerArgsConstructor = constructor.asInstanceOf[Constructor[ScalaSSLEngineProvider]]
      } else if (parameterTypes.length == 2 &&
        classOf[ServerConfig].isAssignableFrom(parameterTypes(0)) &&
        classOf[ApplicationProvider].isAssignableFrom(parameterTypes(1))) {
        serverConfigProviderArgsConstructor = constructor.asInstanceOf[Constructor[ScalaSSLEngineProvider]]
      }
    }

    if (serverConfigProviderArgsConstructor != null) {
      serverConfigProviderArgsConstructor.newInstance(serverConfig, applicationProvider)
    } else if (providerArgsConstructor != null) {
      providerArgsConstructor.newInstance(applicationProvider)
    } else if (noArgsConstructor != null) {
      noArgsConstructor.newInstance()
    } else {
      throw new ClassCastException("No constructor with (appProvider:play.core.ApplicationProvider) or no-args constructor defined!")
    }

  }
}
