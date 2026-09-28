/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.mvc

import org.specs2.mutable.Specification

class SanitizedFilenameSpec extends Specification {

  private def filePart(filename: String) = MultipartFormData.FilePart("upload", filename, None, ())

  "MultipartFormData.FilePart.sanitizedFilename" should {
    "remove Unix, Windows, and mixed directory components" in {
      filePart("../../unix/file.txt").sanitizedFilename must beEqualTo("file.txt")
      filePart("C:\\fakepath\\windows.txt").sanitizedFilename must beEqualTo("windows.txt")
      filePart("../mixed\\path/final.txt").sanitizedFilename must beEqualTo("final.txt")
    }

    "reject filenames without a usable final component" in {
      filePart("..").sanitizedFilename must throwA[IllegalArgumentException]
      filePart("").sanitizedFilename must throwA[IllegalArgumentException]
      filePart(null).sanitizedFilename must throwA[IllegalArgumentException]
    }
  }
}
