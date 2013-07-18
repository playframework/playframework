import sbt._

object ZipHelper {

  /**
   * Uses the native zip command to zip files, because the native command likely supports UNIX permissions.
   */
  def zipNative(dir: File, outputZip: File): Unit = {
    Process(Seq("zip", "-r", outputZip.getAbsolutePath, "."), dir).! match {
      case 0 => ()
      case n => sys.error("Failed to run native zip application!")
    }
  }
}