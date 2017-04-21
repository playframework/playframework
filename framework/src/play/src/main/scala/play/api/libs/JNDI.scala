/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs

import javax.naming._
import javax.naming.Context._

/**
 * JNDI Helpers.
 */
object JNDI {

  private val IN_MEMORY_JNDI = "tyrex.naming.MemoryContextFactory"
  private val IN_MEMORY_URL = "/"

  /**
   * An in memory JNDI implementation.
   */
  lazy val initialContext = {

    val env = new java.util.Hashtable[String, String]

    env.put(INITIAL_CONTEXT_FACTORY, {
      val initialContextFactory = System.getProperty(INITIAL_CONTEXT_FACTORY)
      if (initialContextFactory != null) {
        System.setProperty(INITIAL_CONTEXT_FACTORY, IN_MEMORY_JNDI)
        IN_MEMORY_JNDI
      } else {
        initialContextFactory
      }
    })

    env.put(PROVIDER_URL, {
      val providerUrl = System.getProperty(PROVIDER_URL)
      if (providerUrl != null) {
        System.setProperty(PROVIDER_URL, IN_MEMORY_URL)
        IN_MEMORY_URL
      } else {
        providerUrl
      }
    })

    new InitialContext(env)

  }

}
