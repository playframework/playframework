/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import java.nio.file.Files
import java.nio.file.StandardOpenOption

import scala.concurrent.Future

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import play.api.inject.ApplicationLifecycle
import play.api.Environment

class Module extends AbstractModule {
  override def configure(): Unit = bind(classOf[LifecycleRecorder]).asEagerSingleton()
}

class LifecycleRecorder @Inject() (environment: Environment, lifecycle: ApplicationLifecycle) {
  private def record(name: String): Unit = {
    Files.write(
      environment.getFile(s"target/$name.log").toPath,
      "event\n".getBytes(java.nio.charset.StandardCharsets.UTF_8),
      StandardOpenOption.CREATE,
      StandardOpenOption.APPEND
    )
    ()
  }

  record("application-starts")
  lifecycle.addStopHook { () =>
    record("application-stops")
    Future.successful(())
  }
}
