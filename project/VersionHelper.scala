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

  def versionFmt(out: sbtdynver.GitDescribeOutput): String = {
    if (out.isCleanAfterTag) {
      out.ref.dropPrefix
    } else {
      // Change to increasePatchVersion when not on "main" branch
      VersionHelper.increaseMinorVersion(out.ref.dropPrefix) + "-" + out.commitSuffix.sha + "-SNAPSHOT"
    }
  }

  def fallbackVersion(d: java.util.Date): String = s"HEAD-${sbtdynver.DynVer.timestamp(d)}"

}
