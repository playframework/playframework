/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import java.io.FileWriter
import java.util.Date

import scala.concurrent.Future

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.apache.pekko.actor.ActorSystem
import play.api._
import play.api.inject.ApplicationLifecycle

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure() = {
    val writer = new FileWriter(environment.getFile("target/reload.log"), true)
    writer.write(s"${new Date()} - reloaded\n")
    writer.close()

    bind(classOf[StopRecorder]).asEagerSingleton()
    bind(classOf[TerminationRecorder]).asEagerSingleton()
    if (configuration.getOptional[Boolean]("fail").getOrElse(false)) {
      bind(classOf[FailingComponent]).asEagerSingleton()
    }
  }
}

class StopRecorder @Inject() (environment: Environment, lifecycle: ApplicationLifecycle) {
  lifecycle.addStopHook { () =>
    val writer = new FileWriter(environment.getFile("target/stop.log"), true)
    writer.write(s"${new Date()} - stopped\n")
    writer.close()
    Future.unit
  }
}

class TerminationRecorder @Inject() (environment: Environment, actorSystem: ActorSystem) {
  actorSystem.registerOnTermination {
    val writer = new FileWriter(environment.getFile("target/termination.log"), true)
    writer.write(s"${new Date()} - terminated\n")
    writer.close()
  }
}

// Fails to start after the StopRecorder has registered its stop hook, and the actor system got created
class FailingComponent @Inject() (stopRecorder: StopRecorder, terminationRecorder: TerminationRecorder) {
  throw new RuntimeException("fail=true")
}
