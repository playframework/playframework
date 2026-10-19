/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package sbt

import play.runsupport.DevServer

/** Tracks the server and compilation state owned by a continuous execution. */
private[sbt] class PlayContinuousRun {
  import PlayContinuousRun.*

  private case class Running(owner: Owner, server: DevServer)
  private var current: Option[Running] = None

  def start(
      project: ProjectRef,
      channel: String,
      watch: AnyRef,
      ownerIsWatching: String => Boolean,
      warn: String => Unit
  )(server: => DevServer): Boolean = synchronized {
    prepare(project, channel, Some(watch), ownerIsWatching, warn, canReuse = true) match {
      case Reuse => false
      case _     =>
        current = Some(Running(Owner(project, channel, watch), server))
        true
    }
  }

  /**
   * Prepares a blocking run, which never shares a continuous-run server. `watch` identifies the caller's watch when a
   * run task without Play's termination hook runs inside one.
   */
  def beforeRun(
      project: ProjectRef,
      channel: String,
      watch: Option[AnyRef],
      ownerIsWatching: String => Boolean,
      warn: String => Unit
  ): Unit = synchronized {
    prepare(project, channel, watch, ownerIsWatching, warn, canReuse = false)
    ()
  }

  private def prepare(
      project: ProjectRef,
      channel: String,
      watch: Option[AnyRef],
      ownerIsWatching: String => Boolean,
      warn: String => Unit,
      canReuse: Boolean
  ): Decision = {
    val owner    = current.map(_.owner)
    val decision = decide(owner, project, channel, watch, owner.exists(o => ownerIsWatching(o.channel)))
    (if (decision == Reuse && !canReuse) Reject else decision) match {
      case Reject =>
        val previous = owner.get
        throw new MessageOnlyException(
          if (previous.channel == channel)
            s"Only one Play development server can run per sbt session; project ${previous.project.project} already owns it in this watch, so project ${project.project} cannot start."
          else
            s"Play's development server for project ${previous.project.project} belongs to channel ${previous.channel}, which is still watching. Stop its watch before running project ${project.project}."
        )
      case Restart =>
        val previous = current.get
        current = None
        warn(
          "The previous continuous-run server was still running after its watch ended. " +
            "Stopping it before starting Play again. Check whether Compile / run / watchOnTermination was replaced without preserving Play's cleanup."
        )
        previous.server.close()
        Start
      case decision => decision
    }
  }

  def compile(project: ProjectRef, channel: String): Unit = synchronized {
    current.filter(r => r.owner.project == project && r.owner.channel == channel).foreach(_.server.compile())
  }

  def stop(project: ProjectRef, channel: String): Unit = synchronized {
    current.filter(r => r.owner.project == project && r.owner.channel == channel).foreach { running =>
      current = None
      running.server.close()
    }
  }
}

// Match DevServerRunner's single-server scope across projects and settings reapplication.
// sbt's reload loads the plugin afresh and replaces this object, so termination hooks must still close the server.
private[sbt] object PlayContinuousRun extends PlayContinuousRun {
  private[sbt] case class Owner(project: ProjectRef, channel: String, watch: AnyRef)

  private[sbt] sealed trait Decision
  private[sbt] case object Start   extends Decision
  private[sbt] case object Reuse   extends Decision
  private[sbt] case object Restart extends Decision
  private[sbt] case object Reject  extends Decision

  private[sbt] def decide(
      owner: Option[Owner],
      project: ProjectRef,
      channel: String,
      watch: Option[AnyRef],
      ownerIsWatching: Boolean
  ): Decision = owner match {
    case None                                                                               => Start
    case Some(previous) if previous.channel == channel && watch.exists(_ eq previous.watch) =>
      if (previous.project == project) Reuse else Reject
    case Some(previous) if previous.channel == channel && watch.isDefined => Restart
    // Another channel may be watching a different task after losing its run cleanup.
    // Its watch identity is unavailable here, so preserve its server until that watch ends.
    case Some(_) if ownerIsWatching => Reject
    case Some(_)                    => Restart
  }
}
