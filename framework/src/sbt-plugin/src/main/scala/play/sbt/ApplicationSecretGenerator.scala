/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.sbt

import PlayImport.PlayKeys._
import java.security.SecureRandom
import sbt._

/**
 * Provides tasks for generating and updating application secrets
 */
object ApplicationSecretGenerator {

  def generateSecret = {
    val random = new SecureRandom()

    (1 to 64).map { _ =>
      (random.nextInt(75) + 48).toChar
    }.mkString.replaceAll("\\\\+", "/")
  }

  def generateSecretTask = Def.task[String] {
    val secret = generateSecret
    Keys.streams.value.log.info("Generated new secret: " + secret)
    secret
  }

  private val ApplicationSecret = """\s*(?:(?:application\.secret)|(?:play\.crypto\.secret))\s*[=:].*""".r

  def updateSecretTask = Def.task[File] {
    val secret: String = play.sbt.PlayImport.PlayKeys.generateSecret.value
    val baseDir: File = Keys.baseDirectory.value
    val log = Keys.streams.value.log

    val secretConfig = s"""play.crypto.secret="$secret""""

    val appConfFile = Option(System.getProperty("config.file")) match {
      case Some(applicationConf) => new File(baseDir, applicationConf)
      case None => (Keys.resourceDirectory in Compile).value / "application.conf"
    }

    if (appConfFile.exists()) {
      log.info("Updating application secret in " + appConfFile.getCanonicalPath)
      val lines = IO.readLines(appConfFile)

      val appSecret = lines.find(ApplicationSecret.pattern.matcher(_).matches())

      val newLines = appSecret match {
        case Some(line) =>
          log.info("Replacing old application secret: " + line)
          lines.map {
            case `line` => secretConfig
            case other => other
          }
        case None =>
          log.warn("Did not find application secret in " + appConfFile.getCanonicalPath)
          log.warn("Adding application secret to start of file")
          secretConfig :: lines
      }

      IO.writeLines(appConfFile, newLines)

      appConfFile
    } else {
      log.error("Could not find configuration file at " + appConfFile.getCanonicalPath)
      throw new FeedbackProvidedException {}
    }
  }

}
