/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.websocket

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import scala.jdk.CollectionConverters._

import org.apache.pekko.stream.scaladsl.Flow
import play.api.http.websocket.BinaryMessage
import play.api.http.websocket.Message
import play.api.http.websocket.TextMessage
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.WebSocket
import play.api.test.TestServer
import play.api.Configuration
import play.api.Mode
import play.core.server.NettyServer
import play.core.server.PekkoHttpServer
import play.core.server.ServerConfig
import play.core.server.ServerProvider

object AutobahnWebSocketConformance {

  private val DefaultImage    = "crossbario/autobahn-testsuite:25.10.1"
  private val DefaultPlatform = "linux/amd64"
  private val DefaultHost     = "host.docker.internal"

  private val AcceptedBehavior      = Set("OK", "NON-STRICT", "INFORMATIONAL")
  private val AcceptedCloseBehavior = Set("OK", "INFORMATIONAL")

  private case class Profile(excludedCases: Seq[String])

  private val Profiles = Map(
    "core" -> Profile(Seq("9.*", "12.*", "13.*")),
    "full" -> Profile(Seq("9.*")),
    "all"  -> Profile(Seq.empty)
  )

  private[websocket] case class CaseResult(
      agent: String,
      caseId: String,
      behavior: String,
      closeBehavior: String,
      reportFile: String
  )

  private[websocket] case class ReportEvaluation(
      results: Seq[CaseResult],
      failures: Seq[CaseResult],
      behaviorCounts: Map[String, Int],
      closeBehaviorCounts: Map[String, Int]
  )

  private[websocket] def evaluateReport(report: JsValue): ReportEvaluation = {
    val results = report.as[JsObject].fields.flatMap { case (agent, cases) =>
      cases.as[JsObject].fields.map { case (caseId, result) =>
        CaseResult(
          agent,
          caseId,
          (result \ "behavior").as[String],
          (result \ "behaviorClose").as[String],
          (result \ "reportfile").asOpt[String].getOrElse("")
        )
      }
    }.toVector
    val failures = results.filterNot { result =>
      AcceptedBehavior(result.behavior) && AcceptedCloseBehavior(result.closeBehavior)
    }
    ReportEvaluation(
      results,
      failures,
      results.groupMapReduce(_.behavior)(_ => 1)(_ + _),
      results.groupMapReduce(_.closeBehavior)(_ => 1)(_ + _)
    )
  }

  def main(args: Array[String]): Unit = {
    if (args.length != 2 || !Profiles.contains(args(1))) {
      throw new IllegalArgumentException(
        "Usage: AutobahnWebSocketConformance <netty|pekko-http> <core|full|all>"
      )
    }

    val backend = args(0)
    val provider: ServerProvider = backend match {
      case "netty"      => NettyServer.provider
      case "pekko-http" => PekkoHttpServer.provider
      case _             => throw new IllegalArgumentException(s"Unknown server backend: $backend")
    }
    val profile = Profiles(args(1))

    val reportDirectory = sys.env
      .get("AUTOBAHN_REPORT_DIR")
      .fold(Paths.get("target", "autobahn", backend, args(1)))(Paths.get(_))
      .toAbsolutePath
      .normalize()
    recreateDirectory(reportDirectory)

    val settings = Configuration.from(
      Map(
        "play.server.http.idleTimeout"                       -> "infinite",
        "play.server.https.idleTimeout"                      -> "infinite",
        "play.server.websocket.frame.maxLength"              -> "64m",
        "play.server.websocket.periodic-keep-alive-max-idle" -> "infinite"
      )
    )
    val app = GuiceApplicationBuilder()
      .configure(settings)
      .routes {
        case (_, "/autobahn") =>
          WebSocket.accept[Message, Message] { _ =>
            Flow[Message].collect {
              case text: TextMessage     => text
              case binary: BinaryMessage => binary
            }
          }
      }
      .build()

    val baseServerConfig = ServerConfig(
      rootDir = app.path,
      port = Some(0),
      address = "0.0.0.0",
      mode = Mode.Test
    )
    val serverConfig =
      baseServerConfig.copy(configuration = settings.withFallback(baseServerConfig.configuration))
    val server = TestServer(serverConfig, app, Some(provider))

    server.start()
    try {
      val port            = server.runningHttpPort.get
      val configDirectory = reportDirectory.resolve("config")
      val reports         = reportDirectory.resolve("reports")
      Files.createDirectories(configDirectory)
      Files.createDirectories(reports)

      val cases         = patternsFromEnvironment("AUTOBAHN_CASES", Seq("*"))
      val excludedCases = patternsFromEnvironment("AUTOBAHN_EXCLUDE_CASES", profile.excludedCases)
      val host          = sys.env.getOrElse("AUTOBAHN_HOST", DefaultHost)
      val config = Json.obj(
        "outdir" -> "/reports",
        "servers" -> Json.arr(
          Json.obj(
            "url"   -> s"ws://$host:$port/autobahn",
            "agent" -> s"Play-$backend"
          )
        ),
        "cases"               -> cases,
        "exclude-cases"       -> excludedCases,
        "exclude-agent-cases" -> Json.obj()
      )
      Files.writeString(
        configDirectory.resolve("fuzzingclient.json"),
        Json.prettyPrint(config),
        StandardCharsets.UTF_8
      )

      val command = dockerCommand(configDirectory, reports)
      println(s"Running Autobahn profile '${args(1)}' against Play's $backend backend")
      println(s"Cases: ${cases.mkString(", ")}")
      println(s"Excluded cases: ${excludedCases.mkString(", ")}")
      val exitCode = new ProcessBuilder(command.asJava).inheritIO().start().waitFor()
      if (exitCode != 0) {
        throw new IllegalStateException(s"Autobahn container exited with status $exitCode")
      }

      val reportFile = reports.resolve("index.json")
      if (!Files.isRegularFile(reportFile)) {
        throw new IllegalStateException(s"Autobahn did not create ${reportFile.toAbsolutePath}")
      }
      val evaluation = evaluateReport(Json.parse(Files.readString(reportFile, StandardCharsets.UTF_8)))
      if (evaluation.results.isEmpty) {
        throw new IllegalStateException("Autobahn report did not contain any test results")
      }

      println(s"Autobahn completed ${evaluation.results.size} cases")
      println(s"Behavior: ${formatCounts(evaluation.behaviorCounts)}")
      println(s"Close behavior: ${formatCounts(evaluation.closeBehaviorCounts)}")
      println(s"HTML report: ${reports.resolve("index.html").toAbsolutePath}")

      if (evaluation.failures.nonEmpty) {
        evaluation.failures.take(50).foreach { failure =>
          println(
            s"FAILED ${failure.agent} case ${failure.caseId}: " +
              s"behavior=${failure.behavior}, close=${failure.closeBehavior}, report=${failure.reportFile}"
          )
        }
        if (evaluation.failures.size > 50) {
          println(s"... and ${evaluation.failures.size - 50} more failures")
        }
        throw new IllegalStateException(s"${evaluation.failures.size} Autobahn cases did not conform")
      }
    } finally {
      server.stop()
    }
  }

  private def dockerCommand(configDirectory: Path, reports: Path): Seq[String] = {
    val executable = sys.env.getOrElse("AUTOBAHN_DOCKER", "docker")
    val image      = sys.env.getOrElse("AUTOBAHN_IMAGE", DefaultImage)
    val platform   = sys.env.getOrElse("AUTOBAHN_DOCKER_PLATFORM", DefaultPlatform)
    val user       = sys.env.get("AUTOBAHN_DOCKER_USER").filter(_.nonEmpty).toSeq.flatMap(value => Seq("--user", value))

    Seq(
      executable,
      "run",
      "--rm",
      "--platform",
      platform,
      "--add-host",
      s"$DefaultHost:host-gateway"
    ) ++ user ++ Seq(
      "--volume",
      s"${configDirectory.toAbsolutePath}:/config:ro",
      "--volume",
      s"${reports.toAbsolutePath}:/reports",
      image,
      "wstest",
      "--mode",
      "fuzzingclient",
      "--spec",
      "/config/fuzzingclient.json"
    )
  }

  private def patternsFromEnvironment(name: String, default: Seq[String]): Seq[String] =
    sys.env.get(name).fold(default)(_.split(',').iterator.map(_.trim).filter(_.nonEmpty).toSeq)

  private def formatCounts(counts: Map[String, Int]): String =
    counts.toSeq.sortBy(_._1).map { case (name, count) => s"$name=$count" }.mkString(", ")

  private def recreateDirectory(directory: Path): Unit = {
    if (Files.exists(directory)) {
      val paths = Files.walk(directory)
      try {
        paths.iterator().asScala.toSeq.sortBy(_.getNameCount).reverse.foreach(Files.delete)
      } finally {
        paths.close()
      }
    }
    Files.createDirectories(directory)
  }
}
