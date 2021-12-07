/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs

import javax.naming.Context._
import javax.naming._

/**
 * JNDI Helpers.
 */
object JNDI {
  private val IN_MEMORY_JNDI = "tyrex.naming.MemoryContextFactory"
  private val IN_MEMORY_URL  = "/"

  /**
   * An in memory JNDI implementation.
   *
   * Returns a new InitialContext on every call, and sets the relevant system properties for the in-memory JNDI
   * implementation. InitialContext is NOT thread-safe so instances cannot be shared between threads.
   */
  def initialContext: InitialContext = synchronized {
    val env = new java.util.Hashtable[String, String]

    env.put(
      INITIAL_CONTEXT_FACTORY, {
        val icf = System.getProperty(INITIAL_CONTEXT_FACTORY)
        Option(icf).getOrElse {
          System.setProperty(INITIAL_CONTEXT_FACTORY, IN_MEMORY_JNDI)
          IN_MEMORY_JNDI
        }
      }
    )

    env.put(PROVIDER_URL, {
      val url = System.getProperty(PROVIDER_URL)
      Option(url).getOrElse {
        System.setProperty(PROVIDER_URL, IN_MEMORY_URL)
        IN_MEMORY_URL
      }
    })

    new InitialContext(env)
  }
}
