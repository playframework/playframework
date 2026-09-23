/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption

import play.api.libs.logback.LogbackLoggerConfigurator

class RecordingLoggerConfigurator extends LogbackLoggerConfigurator {
  override def shutdown(): Unit = {
    try {
      super.shutdown()
    } finally {
      val proof = new java.io.File("target/proofs/logger-shutdowns.txt").toPath
      Files.createDirectories(proof.getParent)
      Files.write(
        proof,
        "shutdown\n".getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND
      )
    }
  }
}
