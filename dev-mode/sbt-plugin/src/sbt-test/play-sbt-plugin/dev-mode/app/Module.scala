/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import java.io.FileWriter
import java.util.Date

import com.google.inject.AbstractModule
import play.api._

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure() = {
    val writer = new FileWriter(environment.getFile("target/reload.log"), true)
    writer.write(s"${new Date()} - reloaded\n")
    writer.close()

    if (configuration.getOptional[Boolean]("fail").getOrElse(false)) {
      throw new RuntimeException("fail=true")
    }
  }
}
