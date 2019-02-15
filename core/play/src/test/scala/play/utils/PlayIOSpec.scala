/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.utils

import java.nio.charset.Charset
import java.nio.file.{ Path, Files => JFiles }

import org.specs2.mutable.Specification

class PlayIOSpec extends Specification {

  val utf8 = Charset.forName("UTF8")

  "PlayIO" should {

    "read file content" in {
      val file = JFiles.createTempFile("", "")
      writeFile(file, "file content")

      new String(PlayIO.readFile(file), utf8) must beEqualTo("file content")
    }

    "read file content as a String" in {
      val file = JFiles.createTempFile("", "")
      writeFile(file, "file content")

      PlayIO.readFileAsString(file) must beEqualTo("file content")
    }

    "read url content as a String" in {
      val file = JFiles.createTempFile("", "")
      writeFile(file, "file content")

      val url = file.toUri.toURL

      PlayIO.readUrlAsString(url) must beEqualTo("file content")
    }
  }

  private def writeFile(file: Path, content: String) = {
    if (JFiles.exists(file)) JFiles.delete(file)

    JFiles.createDirectories(file.getParent)
    java.nio.file.Files.write(file, content.getBytes(utf8))
  }

}
