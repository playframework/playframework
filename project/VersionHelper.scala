object VersionHelper {

  private val SemVer = """(\d*)\.(\d*)\.(\d*).*""".r

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

  def versionFmt(out: sbtdynver.GitDescribeOutput, dynverSonatypeSnapshots: Boolean): String = {
    if (out.isCleanAfterTag) {
      out.ref.dropPrefix
    } else {
      val dirtyPart = if (out.isDirty()) out.dirtySuffix.value else ""
      val snapshotPart = if (dynverSonatypeSnapshots && out.isSnapshot()) "-SNAPSHOT" else ""
      VersionHelper.increaseMinorVersion(out.ref.dropPrefix) + "-" + out.commitSuffix.sha + dirtyPart + snapshotPart
    }
  }

  def fallbackVersion(d: java.util.Date): String = s"HEAD-${sbtdynver.DynVer.timestamp(d)}"

}
