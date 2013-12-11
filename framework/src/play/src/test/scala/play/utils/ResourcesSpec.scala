package play.utils

import java.io.{FileInputStream, BufferedInputStream, File, FileOutputStream}
import java.net.URL
import java.util.zip.{ZipEntry, ZipOutputStream}
import org.specs2.mutable.Specification

/**
 * Tests for Resources object
 */
object ResourcesSpec extends Specification {
  import Resources._

  lazy val tmpDir = createTempDir("resources-", ".tmp")
  lazy val jar = File.createTempFile("jar-", ".tmp", tmpDir)
  lazy val fileRes = File.createTempFile("file-", ".tmp", tmpDir)
  lazy val dirRes = createTempDir("dir-", ".tmp", tmpDir)

  sequential
  "resources isDirectory" should {

    step {
      createZip(jar, Seq(fileRes, dirRes))
    }

    "return true for a directory resource URL with the 'file' protocol" in {
      val url = new URL("file", "", dirRes.getAbsolutePath)
      isDirectory(url) must beTrue
    }

    "return false for a file resource URL with the 'file' protocol" in {
      val url = new URL("file", "", fileRes.getAbsolutePath)
      isDirectory(url) must beFalse
    }

    "return true for a directory resource URL with the 'jar' protocol" in {
      val url = new URL("jar", "", s"file:${jar.getAbsolutePath}!/${dirRes.getName}")
      isDirectory(url) must beTrue
    }

    "return false for a file resource URL with the 'jar' protocol" in {
      val url = new URL("jar", "", s"file:${jar.getAbsolutePath}!/${fileRes.getName}")
      isDirectory(url) must beFalse
    }

    "throw an exception for a URL with a protocol other than 'file'/'jar'" in {
      val url = new URL("ftp", "", "/some/path")
      isDirectory(url) must throwAn[IllegalArgumentException]
    }

    step {
      tmpDir.listFiles().foreach(f => f.delete())
      tmpDir.delete()
    }
  }

  private def createTempDir(prefix: String, suffix: String, parent: File = null) = {
    val f = File.createTempFile(prefix, suffix, parent)
    f.delete()
    f.mkdir()
    f
  }

  private def createZip(zip: File, files: Seq[File]) = {
    val zipOutputStream = new ZipOutputStream(new FileOutputStream(zip))
    addFileToZip(zipOutputStream, fileRes)
    addFileToZip(zipOutputStream, dirRes)
    zipOutputStream.close()
  }

  private def addFileToZip(zip: ZipOutputStream, file: File) = {
    val entryName =
      if (file.isDirectory) file.getName + "/"
      else file.getName

    zip.putNextEntry(new ZipEntry(entryName))

    if (!file.isDirectory) {
      val in = new BufferedInputStream(new FileInputStream(file))
      var b = in.read()
      while (b > -1) {
        zip.write(b)
        b = in.read()
      }
      in.close()
    }

    zip.closeEntry()
  }
}
