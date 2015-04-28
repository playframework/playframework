/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs

import java.io._
import java.nio.file.{ FileAlreadyExistsException, StandardCopyOption }

/**
 * FileSystem utilities.
 */
object Files {

  /**
   * A temporary file hold a reference to a real file, and will delete
   * it when the reference is garbaged.
   */
  case class TemporaryFile(file: File) {

    /**
     * Clean this temporary file now.
     */
    def clean(): Boolean = {
      file.delete()
    }

    /**
     * Move the file.
     */
    def moveTo(to: File, replace: Boolean = false): File = {
      try {
        if (replace)
          java.nio.file.Files.move(file.toPath, to.toPath, StandardCopyOption.REPLACE_EXISTING)
        else
          java.nio.file.Files.move(file.toPath, to.toPath)
      } catch {
        case ex: FileAlreadyExistsException => to
      }

      to
    }

    /**
     * Delete this file on garbage collection.
     */
    override def finalize() {
      clean()
    }

  }

  /**
   * Utilities to manage temporary files.
   */
  object TemporaryFile {

    /**
     * Create a new temporary file.
     *
     * Example:
     * {{{
     * val tempFile = TemporaryFile(prefix = "uploaded")
     * }}}
     *
     * @param prefix The prefix used for the temporary file name.
     * @param suffix The suffix used for the temporary file name.
     * @return A temporary file instance.
     */
    def apply(prefix: String = "", suffix: String = ""): TemporaryFile = {
      new TemporaryFile(File.createTempFile(prefix, suffix))
    }

  }
}
