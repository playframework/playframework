/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package sdatabase

import javax.inject._
import akka.actor.ActorSystem
import play.api.libs.concurrent.CustomExecutionContext

@Singleton
class DatabaseExecutionContext @Inject()(system: ActorSystem)
    extends CustomExecutionContext(system, "database.dispatcher")
