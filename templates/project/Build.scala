package play.sbt.activator

import sbt._
import sbt.Keys._
import com.typesafe.sbt.S3Plugin._
import sbt.complete.{Parsers, Parser}

object Templates {

  val templates = SettingKey[Seq[File]]("activator-templates")
  val templateParameters = SettingKey[Map[String, String]]("template-parameters")
  val gitHash = TaskKey[String]("git-hash")
  val nonce = TaskKey[Long]("nonce")

  val syncTemplateDir = SettingKey[File]("sync-template-dir")
  val syncTemplates = TaskKey[Seq[File]]("sync-templates")

  val prepareTemplates = TaskKey[Seq[File]]("prepare-templates")
  val testTemplates = TaskKey[Unit]("test-templates")
  val zipTemplates = TaskKey[Seq[File]]("zip-templates")
  val publishTemplatesTo = SettingKey[String]("publish-templates-to")
  val doPublishTemplates = TaskKey[Boolean]("do-publish-templates")
  val publishTemplates = TaskKey[Unit]("publish-templates")

  val templateSettings: Seq[Setting[_]] = s3Settings ++ Seq(
    templates := Nil,
    templateParameters := Map.empty,
    syncTemplateDir := target.value / "templates",

    watchSources := templates.value.flatMap(_.***.get),

    syncTemplates := {
      val templates: Seq[File] = prepareTemplates.value
      val templateDir: File = syncTemplateDir.value
      val mappings = templates.flatMap { template =>
        (template.***.filter(!_.isDirectory) x relativeTo(template)).map(f => (f._1, templateDir / template.getName / f._2))
      }

      Sync(streams.value.cacheDirectory / "prepared-templates")(mappings)

      templates.map { template =>
        templateDir / template.getName
      }.toSeq
    },

    prepareTemplates := {
      val templateDirs: Seq[File] = templates.value
      val params: Map[String, String] = templateParameters.value
      val outDir: File = target.value / "prepared-templates"

      streams.value.log.info("Preparing templates for Play " + params("PLAY_VERSION") + " with Scala " + params("SCALA_VERSION"))

      // Don't sync directories or .gitkeep files. We can remove
      // .gitkeep files. These files are only there to make sure we preserve
      // directories in Git, but they're not needed in the templates.
      def fileFilter(f: File) = { !f.isDirectory && (f.getName != ".gitkeep") }

      val mappings = templateDirs.flatMap { template =>
        (template.***.filter(fileFilter(_)) x relativeTo(template)).map(f => (f._1, outDir / template.getName / f._2))
      }

      Sync(streams.value.cacheDirectory / "prepared-templates")(mappings)

      mappings.foreach {
        case (original, file) =>
          val contents = IO.read(original)
          val newContents = params.foldLeft(contents) { (str, param) =>
            str.replace("%" + param._1 + "%", param._2)
          }
          if (newContents != contents) {
            IO.write(file, newContents)
          }
        case _ =>
      }

      templateDirs.map { template =>
        outDir / template.getName
      }.toSeq
    },

    testTemplates := {
      val preparedTemplates = syncTemplates.value
      val testDir = target.value / "template-tests"
      val build = (baseDirectory.value.getParentFile / "framework" / "build").getCanonicalPath
      preparedTemplates.foreach { template =>
        val templateDir = testDir / template.getName
        IO.delete(templateDir)
        IO.copyDirectory(template, templateDir)
        streams.value.log.info("Testing template: " + template.getName)
        @volatile var out = List.empty[String]
        val rc = Process(build + " test", templateDir).!(StdOutLogger { s => out = s :: out })
        if (rc != 0) {
          out.reverse.foreach(println)
          streams.value.log.error("Template " + template.getName + " failed to build")
          throw new TemplateBuildFailed(template.getName)
        }
      }
    },

    zipTemplates := {
      streams.value.log.info("Packaging templates...")
      val preparedTemplates = prepareTemplates.value
      val distDir = target.value / "dist-templates"
      preparedTemplates.map { template =>
        val zipFile = distDir / (template.getName + ".zip")
        val files = template.***.filter(!_.isDirectory) x relativeTo(template)
        IO.zip(files, zipFile)
        zipFile
      }
    },

    gitHash := "git rev-parse HEAD".!!.trim,
    nonce := System.currentTimeMillis, 

    S3.host in S3.upload := "downloads.typesafe.com.s3.amazonaws.com",
    S3.progress in S3.upload := true,
    mappings in S3.upload := {
      val zippedTemplates = zipTemplates.value
      // We use the git hash to uniquely identify this upload, and also a nonce, so that if we attempt
      // multiple times to publish the same commit, cloudfront proxy doesn't cache it
      val templateDir = s"play/templates/${gitHash.value}/${nonce.value}/"
      streams.value.log.info("Uploading templates to S3...")
      streams.value.log.info("S3 folder name: " + templateDir)
      zippedTemplates.map { template =>
        (template, templateDir + template.getName)
      }
    },
    S3.host in S3.delete := "downloads.typesafe.com.s3.amazonaws.com",
    S3.keys in S3.delete := {
      val templateDirs = templates.value
      val templateDir = s"play/templates/${gitHash.value}/${nonce.value}/"
      templateDirs.map { template =>
        s"$templateDir${template.getName}.zip"
      }
    },

    publishTemplatesTo := "typesafe.com",
    doPublishTemplates := {
      val host = publishTemplatesTo.value
      val creds = Credentials.forHost(credentials.value, host).getOrElse {
        sys.error("Could not find credentials for host: " + host)
      }
      val upload = S3.upload.value // Ignore result
      val uploaded = (mappings in S3.upload).value.map(m => m._1.getName -> m._2)
      val logger = streams.value.log

      logger.info("Publishing templates...")

      import play.api.libs.ws._
      import play.api.libs.ws.ning.NingWSClient
      import play.api.libs.json._
      import com.ning.http.client.AsyncHttpClientConfig.Builder
      import java.util.Timer
      import java.util.TimerTask
      import scala.concurrent.duration._
      import scala.concurrent._
      import scala.concurrent.ExecutionContext.Implicits.global

      val timer = new Timer()
      val client = new NingWSClient(new Builder().build())
      try {

        def clientCall(path: String): WSRequestHolder = client.url(s"https://$host$path")
          .withAuth(creds.userName, creds.passwd, WSAuthScheme.BASIC)

        def timeout(duration: FiniteDuration): Future[Unit] = {
          val promise = Promise[Unit]()
          timer.schedule(new TimerTask() {
            def run = promise.success(())
          }, duration.toMillis)
          promise.future
        }
       
        def waitUntilNotPending(uuid: String, statusUrl: String): Future[Either[String, String]] = {
          val status: Future[TemplateStatus] = for {
            _ <- timeout(2.seconds)
            resp <- clientCall(statusUrl).withHeaders("Accept" -> "application/json,text/html;q=0.9").get()
          } yield {
            resp.header("Content-Type") match {
              case Some(json) if json.startsWith("application/json") =>
                val js = resp.json
                (js \ "status").as[String] match {
                  case "pending" => TemplatePending(uuid)
                  case "validated" => TemplateValidated(uuid)
                  case "failed" => TemplateFailed(uuid, (js \ "errors").as[Seq[String]])
                }
              case _ =>
                val body = resp.body
                body match {
                  case pending if body.contains("This template is being processed.") => TemplatePending(uuid)
                  case validated if body.contains("This template was published successfully!") => TemplateValidated(uuid)
                  case failed if body.contains("This template failed to publish.") =>
                    TemplateFailed(uuid, extractErrors(body))
                }
            }
          }

          status.flatMap {
            case TemplatePending(uuid) => waitUntilNotPending(uuid, statusUrl)
            case TemplateValidated(_) => Future.successful(Right(uuid))
            case TemplateFailed(_, errors) => Future.successful(Left(errors.mkString("\n")))
          }
        }

        val futures: Seq[Future[(String, String, Either[String, String])]] = uploaded.map {
          case (name, key) =>
            clientCall("/activator/template/publish")
              .post(s"url=http://downloads.typesafe.com/$key").flatMap { resp =>
                if (resp.status != 200) {
                  logger.error("Error publishing template " + name)
                  logger.error("Status code was: " + resp.status)
                  logger.error("Body was: " + resp.body)
                  throw new RuntimeException("Error publishing template")
                }
                val js = resp.json
                val uuid = (js \ "uuid").as[String]
                val statusUrl = (for {
                  links <- (js \ "_links").asOpt[JsObject]
                  status <- (links \ "activator/templates/status").asOpt[JsObject]
                  url <- (status \ "href").asOpt[String]
                } yield url).getOrElse(s"/activator/template/status/$uuid")
                waitUntilNotPending(uuid, statusUrl)
              }.map(result => (name, key, result))
        }

        val results = Await.result(Future.sequence(futures), 1.hour)

        results.foldLeft(true) { (overall, result) =>
          result match {
            case (name, key, Left(error)) =>
              logger.error("Error publishing template " + name)
              logger.error(error)
              false
            case (name, key, Right(uuid)) =>
              logger.info("Template " + name + " published successfully with uuid: " + uuid)
              overall
          }
        }
      } finally {
        timer.cancel()
        client.close()
      }

    },

    publishTemplates <<= (doPublishTemplates, S3.delete, streams).apply { (dpt, s3delete, s) =>
      for {
        streams <- s
        result <- dpt
        _ <- {
          streams.log.info("Cleaning up S3...") 
          s3delete
        }
      } yield result match {
        case true => ()
        case false => throw new TemplatePublishFailed
      }
    },

    commands += templatesCommand
  )

  val templatesCommand = Command("templates", Help.more("templates", "Execute the given command for the given templates"))(templatesParser) { (state, args) =>
    val (templateDirs, command) = args
    val extracted = Project.extract(state)

    def createSetCommand(dirs: Seq[File]): String = {
      dirs.map("file(\"" + _.getAbsolutePath + "\")")
        .mkString("set play.sbt.activator.Templates.templates := Seq(", ",", ")")
    }

    val setCommand = createSetCommand(templateDirs)
    val setBack = templates in extracted.currentRef get extracted.structure.data map createSetCommand toList

    if (command == "") setCommand :: state
    else setCommand :: command :: setBack ::: state
  }

  private def templatesParser(state: State): Parser[(Seq[File], String)] = {
    import Parser._
    import Parsers._
    val templates = Project.extract(state).get(Templates.templates)
    val templateParser = Parsers.OpOrID
      .examples(templates.map(_.getName): _*)
      .flatMap { name =>
        templates.find(_.getName == name) match {
          case Some(templateDir) => success(templateDir)
          case None => failure("No template with name " + name)
        }
      }
    (Space ~> rep1sep(templateParser, Space)) ~ (token(Space ~> matched(state.combinedParser)) ?? "")
  }

  private class TemplateBuildFailed(template: String) extends RuntimeException(template) with FeedbackProvidedException
  private class TemplatePublishFailed extends RuntimeException with FeedbackProvidedException

  private object StdOutLogger {
    def apply(log: String => Unit) = new ProcessLogger {
      def info(s: => String) = log(s)
      def error(s: => String) = System.err.println(s)
      def buffer[T](f: => T) = f
    }
  }

  private sealed trait TemplateStatus
  private case class TemplateValidated(uuid: String) extends TemplateStatus
  private case class TemplateFailed(uuid: String, errors: Seq[String]) extends TemplateStatus
  private case class TemplatePending(uuid: String) extends TemplateStatus

  private def extractErrors(body: String) = body.split("\n")
    .dropWhile(!_.contains("This template failed to publish."))
    .drop(1)
    .takeWhile(!_.contains("</article>"))
    .map(_.trim)
    .filterNot(_.isEmpty)
    .map(_.replaceAll("<p>", "").replaceAll("</p>", ""))
}
