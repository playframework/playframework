/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._
import sbt.Keys._
import sbt.complete.{Parsers, Parser}

import scala.util.Try

object Release {

  val branchVersion = SettingKey[String]("branch-version", "The version to use if Play is on a branch.")

  def settings: Seq[Setting[_]] = Seq(

    version in ThisBuild := {
      // First see if we're on a tag
      val currentTag = Try("git describe --exact-match HEAD".!!(DevNullLogger).trim)
      // Make sure the tag looks like a version number - one example of where it won't is on jenkins, it will be
      // jenkins-play-master-PRS-1234
      currentTag.filter(_.head.isDigit)
        // Otherwise use the fallback version
        .getOrElse((branchVersion in ThisBuild).value)
    },

    commands += releaseCommand
  )

  /**
   * The release command.
   *
   * Releasing is done in the following stages:
   *
   * - verify - verifies that a release can happen
   * - tag - tags the git repo
   * - test - runs the tests
   * - publish - publishes the changes
   * - bump - bumps the version for the next development iteration
   * - push - pushes the tag and change
   *
   * To start the release, run
   *
   * {{{
   *   release <version-number> [-next <next-version-number>]
   * }}}
   *
   * If one part of the process fails, the process can be resumed by supplying the stage name to the command.
   *
   * For example, if the test stage fails, but you want to resume anyway, you can say:
   *
   * {{{
   *   release publish
   * }}}
   */
  val releaseCommand: Command = Command("release", Help.more("release", "Release Play"))(_ => releaseParser) { (state, options) =>
    val stage = options.stage.getOrElse(VerifyRelease)

    val nextCommands = stage.execute(state, options)

    val nextStage = orderedStages.dropWhile(_.name != stage.name).drop(1).headOption
    val nextStageCommand = options.copy(stage = nextStage).toReleaseCommand

    nextCommands ::: nextStageCommand.toList ::: state
  }

  val releaseParser: Parser[ReleaseOptions] = {
    import Parsers._
    import Parser._

    trait ReleaseOption
    case class NextVersion(version: String) extends ReleaseOption
    case class Version(version: String) extends ReleaseOption
    case class StageOption(stage: ReleaseStage) extends ReleaseOption
    case object DryRunOption extends ReleaseOption
    case object SkipTestsOption extends ReleaseOption

    val nextVersionParser = literal("-next") ~> Space ~> NotSpace.map(NextVersion)
    val dryRunParser = literal("-dry-run").map(_ => DryRunOption)
    val skipTestsParser = literal("-skip-tests").map(_ => SkipTestsOption)
    val releaseStageParser = allStages.map(stage => literal(stage.name).map(_ => StageOption(stage))).reduce(_ | _)
    val versionParser = NotSpace.map(Version)

    Space ~> repsep(nextVersionParser | dryRunParser | skipTestsParser | releaseStageParser | versionParser, Space).flatMap { options =>
      val stages = options.collect { case StageOption(s) => s }
      val versions = options.collect { case Version(v) => v }
      val nextVersions = options.collect { case NextVersion(v) => v }
      val dryRun = options.contains(DryRunOption)
      val skipTests = options.contains(SkipTestsOption)

      def verify(name: String, seq: Seq[_]): Option[Parser[ReleaseOptions]] = {
        if (seq.size > 1) Some(failure(s"Multiple $name specified: ${seq.mkString(" ,")}"))
        else None
      }

      verify("stages", stages.view.map(_.name)) orElse
        verify("versions", versions) orElse
        verify("next versions", nextVersions) getOrElse {
        success(ReleaseOptions(stages.headOption, versions.headOption, nextVersions.headOption, dryRun, skipTests))
      }
    }.examples(allStages.map(_.name).toSeq ++ Seq("-next", "-dry-run"): _*)

  }

  case class ReleaseOptions(stage: Option[ReleaseStage],
                            version: Option[String],
                            nextVersion: Option[String],
                            dryRun: Boolean,
                            skipTests: Boolean) {
    def toReleaseCommand: Option[String] = {
      stage.map { stage =>
        val next = nextVersion.map(" -next " + _).getOrElse("")
        val v = version.map(" " + _).getOrElse("")
        val dr = if (dryRun) " -dry-run" else ""
        val st = if (skipTests) " -skip-tests" else ""
        s"release ${stage.name}$dr$st$v$next"
      }
    }
  }


  sealed trait ReleaseStage {
    val name: String

    /**
     * Execute the release stage.
     *
     * @param state The sbt state.
     * @param options The release options.
     * @return Any extra commands that should be run before the next command is run.
     */
    def execute(state: State, options: ReleaseOptions): Seq[String]
  }

  object VerifyRelease extends ReleaseStage {
    val name = "verify"
    def execute(state: State, options: ReleaseOptions) = {
      val version = options.version match {
        case Some(v) =>
          val nextVersion = options.nextVersion.getOrElse(workOutNextVersion(v))
          if (options.dryRun) {
            state.log.info(s"Dry run of release for Play $v and setting next version to $nextVersion")
          } else {
            state.log.info(s"Releasing Play $v and setting next version to $nextVersion")
          }
          v
        case None =>
          error(state, "You must specify the version you want to release")
      }

      if ("git diff HEAD --quiet".! > 0) {
        error(state, "There are uncommitted changes")
      }

      val currentBranch = "git rev-parse --abbrev-ref HEAD".!!.trim

      if (currentBranch == "HEAD") {
        error(state, "Not on a git branch")
      }

      if (s"git config branch.$currentBranch.merge".! > 0 || s"git config branch.$currentBranch.remote".! > 0) {
        error(state, s"No remote tracking branch configured for $currentBranch")
      }

      if (s"git tag -l $version".!!.trim.nonEmpty) {
        error(state, s"Tag $version already exists")
      }

      Nil
    }
  }

  object TagRelease extends ReleaseStage {
    val name = "tag"
    def execute(state: State, options: ReleaseOptions) = {
      options.version match {
        case Some(version) =>
          state.log.info(s"Tagging $version")
          Seq("git", "tag", "-a", "-s", "-m", s"Releasing $version", version).!!
        case None =>
          error(state, "You must specify the version you want to release")
      }

      Seq("reload")
    }
  }

  object TestRelease extends ReleaseStage {
    val name: String = "test"
    def execute(state: State, options: ReleaseOptions) = {
      if (options.skipTests) {
        state.log.info("Skipping tests")
        Nil
      } else {
        Seq("clean", "+test")
      }
    }
  }

  object PublishRelease extends ReleaseStage {
    val name: String = "publish"
    def execute(state: State, options: ReleaseOptions) = {
      if (options.dryRun) {
        Seq("+publishLocalSigned")
      } else {
        Seq("+publishSigned")
      }
    }
  }

  object BumpRelease extends ReleaseStage {
    val name: String = "bump"
    def execute(state: State, options: ReleaseOptions) = {
      val extracted = Project.extract(state)
      val nextVersion = options.nextVersion.getOrElse {
        val version = options.version.getOrElse(extracted.get(Keys.version in ThisBuild))
        workOutNextVersion(version)
      }
      val oldBranchVersion = extracted.get(branchVersion in ThisBuild)

      if (nextVersion != oldBranchVersion) {

        state.log.info("Bumping Play version to " + nextVersion)

        val baseDir = extracted.get(baseDirectory in ThisBuild)
        val versionFile = baseDir / "version.sbt"

        IO.write(versionFile, "Release.branchVersion in ThisBuild := \"" + nextVersion + "\"\n")

        if ("git diff HEAD --quiet".! > 0) {
          "git add --all".!!
          Seq("git", "commit", "-m", s"Setting version to $nextVersion").!!
        }
        Seq("reload")
      } else {
        Nil
      }
    }
  }

  object PushRelease extends ReleaseStage {
    val name = "push"
    def execute(state: State, options: ReleaseOptions) = {
      if (options.dryRun) {
        state.log.info("Not pushing tags because this is a dry run")
      } else {
        val tags = options.version.getOrElse("--tags")
        if (s"git push origin $tags".!(errorToOutLogger(state.log)) > 0) {
          error(state, "Error pushing tags")
        }

        val currentBranch = "git rev-parse --abbrev-ref HEAD".!!.trim

        if (s"git push origin $currentBranch".!(errorToOutLogger(state.log)) > 0) {
          error(state, s"Error pushing $currentBranch")
        }
      }

      Nil
    }
  }

  def workOutNextVersion(version: String) = {
    val components = version.split('.')
    val first = components.dropRight(1).mkString(".")
    val last = components.last
    if (last.matches("[0-9]+")) {
      val next = last.toInt + 1
      s"$first.$next-SNAPSHOT"
    } else {
      val next = last.takeWhile(_.isDigit)
      s"$first.$next-SNAPSHOT"
    }
  }

  lazy val allStages: Set[ReleaseStage] = orderedStages.toSet

  lazy val orderedStages: Seq[ReleaseStage] = Seq(
    VerifyRelease,
    TagRelease,
    TestRelease,
    PublishRelease,
    BumpRelease,
    PushRelease
  )


  def errorToOutLogger(logger: Logger): ProcessLogger = new ProcessLogger {
    def info(s: => String) = logger.info(s)
    def error(s: => String) = logger.info(s)
    def buffer[T](f: => T) = f
  }

  object DevNullLogger extends ProcessLogger {
    def info(s: => String) = ()
    def error(s: => String) = ()
    def buffer[T](f: => T) = f
  }

  object ReleaseException extends FeedbackProvidedException

  def error(state: State, msg: String): Nothing = {
    state.log.error(msg)
    throw ReleaseException
  }
}
