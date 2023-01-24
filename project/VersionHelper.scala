/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import scala.sys.process.Process
import scala.sys.process.ProcessLogger

object VersionHelper {

  private val SemVer           = """(\d*)\.(\d*)\.(\d*).*""".r
  private val SemVerPreVersion = """(\d*)\.(\d*)\.(\d*)-(M|RC)(\d*)""".r

  // For main branch
  private def increaseMinorVersion(tag: String): String = {
    tag match {
      case SemVer(major, minor, patch) =>
        s"$major.${minor.toInt + 1}.0"
      case _ =>
        tag
    }
  }

  // For version branches
  private def increasePatchVersion(tag: String): String = {
    tag match {
      case SemVer(major, minor, patch) =>
        s"$major.$minor.${patch.toInt + 1}"
      case _ =>
        tag
    }
  }

  // For release candiates (-RC) and milestones (-M), no matter which branch
  private def increasePreVersion(tag: String): String = {
    tag match {
      case SemVerPreVersion(major, minor, patch, preVersionType, preVersion) =>
        s"$major.$minor.$patch-$preVersionType${preVersion.toInt + 1}"
      case _ =>
        tag
    }
  }

  def versionFmt(out: sbtdynver.GitDescribeOutput, dynverSonatypeSnapshots: Boolean): String = {
    val versionNoPrefix = out.ref.value.substring(1) // remove the v... prefix
    if (out.isCleanAfterTag) {
      versionNoPrefix
    } else {
      val dirtyPart    = if (out.isDirty()) out.dirtySuffix.value else ""
      val snapshotPart = if (dynverSonatypeSnapshots && out.isSnapshot()) "-SNAPSHOT" else ""
      (if (versionNoPrefix.matches(""".*-(M|RC)\d+$""")) {
         // tag is a milestone or release candidate, therefore we increase the version after the -RC or -M (e.g. -RC1 becomes -RC2)
         // it does not matter on which branch we are on
         VersionHelper.increasePreVersion(versionNoPrefix)
       } else {
         val mainBranchIsAncestor =
           Process("git merge-base --is-ancestor main HEAD").run(ProcessLogger(_ => ())).exitValue() == 0
         lazy val masterBranchIsAncestor =
           Process("git merge-base --is-ancestor master HEAD").run(ProcessLogger(_ => ())).exitValue() == 0
         if (mainBranchIsAncestor || masterBranchIsAncestor) {
           // We are on the main (or master) branch, or a branch that is forked off from the main branch
           VersionHelper.increaseMinorVersion(versionNoPrefix)
         } else {
           // We are not on the main (or master) branch or one off its children.
           // Therefore we are e.g. on 2.8.x or a branch that is forked off from 2.8.x or 2.9.x or ... you get it ;)
           VersionHelper.increasePatchVersion(versionNoPrefix)
         }
       }) + Option(out.commitSuffix.sha).filter(_.nonEmpty).map("-" + _).getOrElse("") + dirtyPart + snapshotPart
    }
  }

  def fallbackVersion(d: java.util.Date): String = s"HEAD-${sbtdynver.DynVer.timestamp(d)}"

}
