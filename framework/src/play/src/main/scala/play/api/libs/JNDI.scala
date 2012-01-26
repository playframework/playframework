package play.api.libs

import play.api._

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
      Play.maybeApplication.flatMap(_.configuration.getString(INITIAL_CONTEXT_FACTORY)).getOrElse {
        System.setProperty(INITIAL_CONTEXT_FACTORY, IN_MEMORY_JNDI)
        IN_MEMORY_JNDI
      }
    })

    env.put(PROVIDER_URL, {
      Play.maybeApplication.flatMap(_.configuration.getString(PROVIDER_URL)).getOrElse {
        System.setProperty(PROVIDER_URL, IN_MEMORY_URL)
        IN_MEMORY_URL
      }
    })

    new InitialContext(env)

  }

}