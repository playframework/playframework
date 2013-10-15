/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs

import java.util.concurrent.atomic.AtomicReference

/**
 * Asynchronous API to to query web services, as an http client.
 */
package object ws {
  private val clientHolder: AtomicReference[Option[WSClient]] = new AtomicReference(None)

  /**
   * resets the underlying AsyncHttpClient
   */
  private[play] def resetClient(): Unit = {
    clientHolder.getAndSet(None).map(_.close())
  }

  /**
   * retrieves or creates underlying HTTP client.
   */
  implicit def wsclient: WSClient = {
    clientHolder.get.getOrElse({
      // A critical section of code. Only one caller has the opportuntity of creating a new client.
      synchronized {
        clientHolder.get match {
          case None => {
            val client: WSClient = NingUtil.newClient
            clientHolder.set(Some(client))
            client
          }
          case Some(client) => client
        }

      }
    })
  }
}