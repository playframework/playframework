/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.utils

import java.io.{ BufferedInputStream, File }
import java.net.{ URL, URLConnection, URLStreamHandler }
import java.util.zip.{ ZipEntry, ZipOutputStream }
import org.specs2.mutable.Specification

import play.api.PlayCoreTestApplication

/**
 * Tests for Resources object
 */
class ResourcesSpec extends Specification {
  import Resources._

  lazy val app = PlayCoreTestApplication()
  lazy val tmpDir = createTempDir("resources-", ".tmp")
  lazy val jar = File.createTempFile("jar-", ".tmp", tmpDir)
  lazy val fileRes = File.createTempFile("file-", ".tmp", tmpDir)
  lazy val dirRes = createTempDir("dir-", ".tmp", tmpDir)
  lazy val dirSpacesRes = createTempDir("dir spaces ", ".tmp", tmpDir)
  lazy val spacesDir = createTempDir("spaces ", ".tmp", tmpDir)
  lazy val spacesJar = File.createTempFile("jar-spaces", ".tmp", spacesDir)
  lazy val resourcesDir = new File(app.classloader.getResource("").getPath)
  lazy val tmpResourcesDir = createTempDir("test-bundle-", ".tmp", resourcesDir)
  lazy val fileBundle = File.createTempFile("file-", ".tmp", tmpResourcesDir)
  lazy val dirBundle = createTempDir("dir-", ".tmp", tmpResourcesDir)
  lazy val spacesDirBundle = createTempDir("dir spaces ", ".tmp", tmpResourcesDir)
  lazy val classloader = app.classloader
  lazy val osgiClassloader = new OsgiClassLoaderSimulator(app.classloader, resourcesDir)

  /* In order to test Resources.isDirectory when the protocol is "bundle://", there are 2 options:
   * a) run the test within an OSGi container (using Pax Exam),
   * b) simulate the behavior of an OSGi class loader (cf. comment in play.utils.Resources). */
  class OsgiClassLoaderSimulator(classloader: ClassLoader, resourcesDir: File) extends ClassLoader {
    override def getResource(name: String): URL = {
      val f = new File(resourcesDir, name)
      val fURL = f.toURI.toURL
      if (!f.exists) null
      else {
        if (name.last == '/')
          if (f.isDirectory) fURL
          else null
        else fURL
      }
    }
  }

  class BundleStreamHandler extends URLStreamHandler {
    def openConnection(u: URL): URLConnection = throw new Exception("should never happen")
  }

  sequential
  "resources isDirectory" should {

    step {
      createZip(jar, Seq(fileRes, dirRes, dirSpacesRes))
      createZip(spacesJar, Seq(fileRes, dirRes, dirSpacesRes))
    }

    "return true for a directory resource URL with the 'file' protocol" in {
      val url = dirRes.toURI.toURL
      isDirectory(classloader, url) must beTrue
    }

    "return false for a file resource URL with the 'file' protocol" in {
      val url = fileRes.toURI.toURL
      isDirectory(classloader, url) must beFalse
    }

    "return true for a directory resource URL that contains spaces with the 'file' protocol" in {
      val url = dirSpacesRes.toURI.toURL
      isDirectory(classloader, url) must beTrue
    }

    "return true for a directory resource URL with the 'jar' protocol" in {
      val url = new URL("jar", "", createJarUrl(jar, dirRes))
      isDirectory(classloader, url) must beTrue
    }

    "return true for a directory resource URL that contains spaces in the jar path with the 'jar' protocol" in {
      val url = new URL("jar", "", createJarUrl(spacesJar, dirRes))
      isDirectory(classloader, url) must beTrue
    }

    "return true for a directory resource URL that contains spaces in the file path with the 'jar' protocol" in {
      val url = new URL("jar", "", createJarUrl(jar, dirSpacesRes))
      isDirectory(classloader, url) must beTrue
    }

    "return false for a file resource URL with the 'jar' protocol" in {
      val url = new URL("jar", "", createJarUrl(jar, fileRes))
      isDirectory(classloader, url) must beFalse
    }

    "return true for a directory resource URL with the 'bundle' protocol" in {
      val relativeIndex = dirBundle.getAbsolutePath.indexOf("test-bundle-")
      val dir = dirBundle.getAbsolutePath.substring(relativeIndex)
      val url = new URL("bundle", "325.0", 25, dir, new BundleStreamHandler)
      isDirectory(osgiClassloader, url) must beTrue
    }

    "return true for a directory resource URL that contains spaces with the 'bundle' protocol" in {
      val relativeIndex = spacesDirBundle.getAbsolutePath.indexOf("test-bundle-")
      val dir = spacesDirBundle.getAbsolutePath.substring(relativeIndex)
      val url = new URL("bundle", "325.0", 25, dir, new BundleStreamHandler)
      isDirectory(osgiClassloader, url) must beTrue
    }

    "return false for a file resource URL with the 'bundle' protocol" in {
      val relativeIndex = fileBundle.getAbsolutePath.indexOf("test-bundle-")
      val file = fileBundle.getAbsolutePath.substring(relativeIndex)
      val url = new URL("bundle", "325.0", 25, file, new BundleStreamHandler)
      isDirectory(osgiClassloader, url) must beFalse
    }

    "return true for a directory resource URL with the 'zip' protocol" in {
      val url = new URL("zip", "", 0, createZipUrl(jar, dirRes), EmptyURLStreamHandler)
      isDirectory(classloader, url) must beTrue
    }

    "return true for a directory resource URL that contains spaces in the zip path with the 'zip' protocol" in {
      val url = new URL("zip", "", 0, createZipUrl(spacesJar, dirRes), EmptyURLStreamHandler)
      isDirectory(classloader, url) must beTrue
    }

    "return true for a directory resource URL that contains spaces in the file path with the 'zip' protocol" in {
      val url = new URL("zip", "", 0, createZipUrl(jar, dirSpacesRes), EmptyURLStreamHandler)
      isDirectory(classloader, url) must beTrue
    }

    "return false for a file resource URL with the 'zip' protocol" in {
      val url = new URL("zip", "", 0, createZipUrl(jar, fileRes), EmptyURLStreamHandler)
      isDirectory(classloader, url) must beFalse
    }

    "throw an exception for a URL with a protocol other than 'file'/'jar'/'zip' / 'bundle'" in {
      val url = new URL("ftp", "", "/some/path")
      isDirectory(classloader, url) must throwAn[IllegalArgumentException]
    }

    step {
      def delete(file: File): Unit = {
        if (file.isDirectory) file.listFiles().foreach(delete)
        file.delete
      }
      delete(tmpDir)
      delete(tmpResourcesDir)
    }
  }

  object EmptyURLStreamHandler extends URLStreamHandler {
    def openConnection(u: URL) = new URLConnection(u) {
      def connect(): Unit = {}
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
    val zipOutputStream = new ZipOutputStream(java.nio.file.Files.newOutputStream(zip.toPath))
    files.foreach(f => addFileToZip(zipOutputStream, f))
    zipOutputStream.close()
  }

  private def addFileToZip(zip: ZipOutputStream, file: File) = {
    val entryName =
      if (file.isDirectory) file.getName + "/"
      else file.getName

    zip.putNextEntry(new ZipEntry(entryName))

    if (!file.isDirectory) {
      val in = new BufferedInputStream(java.nio.file.Files.newInputStream(file.toPath))
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
