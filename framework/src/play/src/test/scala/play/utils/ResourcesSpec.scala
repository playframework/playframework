package play.utils

import java.io.{FileInputStream, BufferedInputStream, File, FileOutputStream}
import java.net.{URI, URLEncoder, URL, URLStreamHandler, URLConnection}
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
  lazy val dirSpacesRes = createTempDir("dir spaces ", ".tmp", tmpDir)
  lazy val spacesDir = createTempDir("spaces ", ".tmp", tmpDir)
  lazy val spacesJar = File.createTempFile("jar-spaces", ".tmp", spacesDir)

  sequential
  "resources isDirectory" should {

    step {
      createZip(jar, Seq(fileRes, dirRes, dirSpacesRes))
      createZip(spacesJar, Seq(fileRes, dirRes, dirSpacesRes))
    }

    "return true for a directory resource URL with the 'file' protocol" in {
      val url = dirRes.toURI.toURL
      isDirectory(url) must beTrue
    }

    "return false for a file resource URL with the 'file' protocol" in {
      val url = fileRes.toURI.toURL
      isDirectory(url) must beFalse
    }

    "return true for a directory resource URL that contains spaces with the 'file' protocol" in {
      val url = dirSpacesRes.toURI.toURL
      isDirectory(url) must beTrue
    }

    "return true for a directory resource URL with the 'jar' protocol" in {
      val url = new URL("jar", "", createJarUrl(jar, dirRes))
      isDirectory(url) must beTrue
    }

    "return true for a directory resource URL that contains spaces in the jar path with the 'jar' protocol" in {
      val url = new URL("jar", "", createJarUrl(spacesJar, dirRes))
      isDirectory(url) must beTrue
    }

    "return true for a directory resource URL that contains spaces in the file path with the 'jar' protocol" in {
      val url = new URL("jar", "", createJarUrl(jar, dirSpacesRes))
      isDirectory(url) must beTrue
    }

    "return false for a file resource URL with the 'jar' protocol" in {
      val url = new URL("jar", "", createJarUrl(jar, fileRes))
      isDirectory(url) must beFalse
    }

    "return true for a directory resource URL with the 'zip' protocol" in {
      val url = new URL("zip", "", 0, createZipUrl(jar, dirRes), EmptyURLStreamHandler)
      isDirectory(url) must beTrue
    }

    "return true for a directory resource URL that contains spaces in the zip path with the 'zip' protocol" in {
      val url = new URL("zip", "", 0, createZipUrl(spacesJar, dirRes), EmptyURLStreamHandler)
      isDirectory(url) must beTrue
    }

    "return true for a directory resource URL that contains spaces in the file path with the 'zip' protocol" in {
      val url = new URL("zip", "", 0, createZipUrl(jar, dirSpacesRes), EmptyURLStreamHandler)
      isDirectory(url) must beTrue
    }

    "return false for a file resource URL with the 'zip' protocol" in {
      val url = new URL("zip", "", 0, createZipUrl(jar, fileRes), EmptyURLStreamHandler)
      isDirectory(url) must beFalse
    }

    "throw an exception for a URL with a protocol other than 'file'/'jar'/'zip'" in {
      val url = new URL("ftp", "", "/some/path")
      isDirectory(url) must throwAn[IllegalArgumentException]
    }

    step {
      def delete(file: File): Unit = {
        if (file.isDirectory) file.listFiles().foreach(delete)
        file.delete
      }
      delete(tmpDir)
    }
  }

  object EmptyURLStreamHandler extends URLStreamHandler {
    def openConnection(u: URL) = new URLConnection(u) {
      def connect() {}
    }
  }

  private def createJarUrl(jarFile: File, file: File) = {
    s"${jarFile.toURI.toURL}!/${UriEncoding.encodePathSegment(file.getName, "utf-8")}"
  }

  private def createZipUrl(zipFile: File, file: File) = {
    s"zip:${zipFile.toURI.toURL}!/${UriEncoding.encodePathSegment(file.getName, "utf-8")}"
  }

  private def createTempDir(prefix: String, suffix: String, parent: File = null) = {
    val f = File.createTempFile(prefix, suffix, parent)
    f.delete()
    f.mkdir()
    f
  }

  private def createZip(zip: File, files: Seq[File]) = {
    val zipOutputStream = new ZipOutputStream(new FileOutputStream(zip))
    files.foreach(f => addFileToZip(zipOutputStream, f))
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
