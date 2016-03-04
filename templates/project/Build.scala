/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.sbt.activator

import sbt._
import sbt.Keys._
import com.typesafe.sbt.S3Plugin._
import sbt.complete.{Parsers, Parser}

/**
 * This must be here, and not in build.sbt, since Project.setSbtFiles only works from a .scala file
 */
object TemplatesBuild extends Build {
  lazy val root = (project in file("."))
    .setSbtFiles(
      // Load the version from Play's version.sbt
      // Order is important, load the version first, then load build.sbt which may modify it with the play.version
      // system property.
      file("../framework/version.sbt"),
      file("./build.sbt")
    )
}

object Templates {

  case class TemplateSources(name: String, mainDir: File, includeDirs: Seq[File], params: Map[String, String]) {
    val sourceDirs: Seq[File] = mainDir +: includeDirs
  }

  val templates = SettingKey[Seq[TemplateSources]]("activatorTemplates")
  val templateParameters = SettingKey[Map[String, String]]("templateParameters")
  val ignoreTemplateFiles = SettingKey[Seq[String]]("ignoreTemplateFiles")
  val gitHash = TaskKey[String]("gitHash")
  val nonce = TaskKey[Long]("nonce")

  val syncTemplateDir = SettingKey[File]("syncTemplateDir")
  val syncTemplates = TaskKey[Seq[File]]("syncTemplates")

  val prepareTemplates = TaskKey[Seq[File]]("prepareTemplates")
  val testTemplates = TaskKey[Unit]("testTemplates")
  val zipTemplates = TaskKey[Seq[File]]("zipTemplates")
  val publishTemplatesTo = SettingKey[String]("publishTemplatesTo")
  val doPublishTemplates = TaskKey[Boolean]("doPublishTemplates")
  val publishTemplates = TaskKey[Unit]("publishTemplates")

  val templateSettings: Seq[Setting[_]] = s3Settings ++ Seq(
    templates := Nil,
    templateParameters := Map.empty,
    ignoreTemplateFiles := Seq.empty,
    syncTemplateDir := target.value / "templates",

    watchSources := templates.value.flatMap(_.sourceDirs.flatMap(_.***.get)),

    // Calls `prepareTemplates` then copies all the files from
    // target/prepared-templates/$template to target/templates/$template.
    syncTemplates := {
      val preparedTemplates: Seq[File] = prepareTemplates.value
      val syncTemplateDirValue: File = syncTemplateDir.value
      val mappings: Seq[(File, File)] = preparedTemplates.flatMap { template =>
        (template.***.filter(!_.isDirectory) x relativeTo(template)).map { f =>
          (f._1, syncTemplateDirValue / template.getName / f._2)
        }
      }

      // Sync files, caching sync data in file "prepared-templates".
      Sync(streams.value.cacheDirectory / "prepared-templates")(mappings)

      preparedTemplates.map { template =>
        syncTemplateDirValue / template.getName
      }.toSeq
    },

    // Syncs templates from each template dir to target/prepared-templates/$template.
    // Replaces all the template parameters (%SCALA_VERSION%, etc) in the copied
    // files.
    //
    // Returns list of target/prepared-templates/$template directories.
    prepareTemplates := {
      val templateSourcesList: Seq[TemplateSources] = templates.value
      val params: Map[String, String] = templateParameters.value
      val ignore: Set[String] = ignoreTemplateFiles.value.to[Set]
      val outDir: File = target.value / "prepared-templates"

      streams.value.log.info("Preparing templates for Play " + params("PLAY_VERSION") + " with Scala " + params("SCALA_VERSION"))

      // Don't sync directories or .gitkeep files. We can remove
      // .gitkeep files. These files are only there to make sure we preserve
      // directories in Git, but they're not needed in the templates.
      def fileFilter(f: File) = { !f.isDirectory && (f.getName != ".gitkeep") }

      val mappings: Seq[(File, File)] = templateSourcesList.flatMap {
        case templateSources: TemplateSources =>
          val relativeMappings: Seq[(File,String)] = templateSources.sourceDirs.flatMap(dir => dir.***.filter(fileFilter(_)) x relativeTo(dir))
          // Rebase the files onto the target directory, also filtering out ignored files
          relativeMappings.collect {
            case (orig, targ) if !ignore.contains(orig.getName) =>
              (orig, outDir / templateSources.name / targ)
          }
      }

      // Sync files, caching sync data in file "prepared-templates".
      Sync(streams.value.cacheDirectory / "prepared-templates")(mappings)

      // Replace parameters like %SCALA_VERSION% in each of the template files.
      mappings.foreach {
        case (original, file) =>
          // Build a map of parameters to replace
          val templateParams: Map[String, String] = {
            // Find the template name by looking in the path of the target file
            @scala.annotation.tailrec
            def topDir(f: java.io.File): File = {
              val parent: java.io.File = f.getParentFile
              if (f.getParent == null) f else topDir(parent)
            }
            val rebasedFile = (file relativeTo outDir).getOrElse(sys.error(s"Can't rebase prepared template $file to dir $outDir"))
            val templateName: String = topDir(rebasedFile).getName
            val templateSources: TemplateSources = templateSourcesList.find(_.name == templateName).getOrElse(sys.error(s"Couldn't find template named $templateName"))
            templateSources.params
          }
          val allParams: Map[String, String] = params ++ templateParams

          // Read the file and replace the parameters
          val contents = IO.read(original)
          val newContents = allParams.foldLeft(contents) { (str, param) =>
            str.replace("%" + param._1 + "%", param._2)
          }
          if (newContents != contents) {
            IO.write(file, newContents)
          }
        case _ =>
      }

      templateSourcesList.map { templateSources =>
        outDir / templateSources.name
      }.toSeq
    },

    testTemplates := {
      val preparedTemplates = syncTemplates.value
      val testDir = target.value / "template-tests"
      preparedTemplates.foreach { template =>
        val templateDir = testDir / template.getName
        IO.delete(templateDir)
        IO.copyDirectory(template, templateDir)
        streams.value.log.info("Testing template: " + template.getName)
        @volatile var out = List.empty[String]
        val rc = Process("sbt test", templateDir).!(StdOutLogger { s => out = s :: out })
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
      val templateSourcesList = templates.value
      val templateDir = s"play/templates/${gitHash.value}/${nonce.value}/"
      templateSourcesList.map { templateSources =>
        s"$templateDir${templateSources.name}.zip"
      }
    },

    publishTemplatesTo := "www.lightbend.com",
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

    def createSetCommand(dirs: Seq[TemplateSources]): String = {
      dirs.map("file(\"" + _.mainDir.getAbsolutePath + "\")")
        .mkString("set play.sbt.activator.Templates.templates := Seq(", ",", ")")
    }

    val setCommand = createSetCommand(templateDirs)
    val setBack = templates in extracted.currentRef get extracted.structure.data map createSetCommand toList

    if (command == "") setCommand :: state
    else setCommand :: command :: setBack ::: state
  }

  private def templatesParser(state: State): Parser[(Seq[TemplateSources], String)] = {
    import Parser._
    import Parsers._
    val templateSourcesList: Seq[TemplateSources] = Project.extract(state).get(Templates.templates)
    val templateParser = Parsers.OpOrID
      .examples(templateSourcesList.map(_.name): _*)
      .flatMap { name =>
        templateSourcesList.find(_.name == name) match {
          case Some(templateSources) => success(templateSources)
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

  play.api.Logger.configure(play.api.Environment.simple())
}
