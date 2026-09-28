/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.parsers;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.pekko.annotation.InternalApi;

/** Internal support for safely extracting a final component from an uploaded filename. */
@InternalApi
public final class MultipartFileName {

  private static final String INVALID_FILENAME_MESSAGE =
      "The uploaded filename does not contain a usable final path component";

  private MultipartFileName() {}

  /**
   * Removes directory components using both common separator characters.
   *
   * @param filename an untrusted multipart filename
   * @return the normalized final path component
   * @throws IllegalArgumentException if the filename has no usable final component or is not a
   *     valid path
   */
  public static String sanitize(String filename) {
    if (filename == null) {
      throw new IllegalArgumentException(INVALID_FILENAME_MESSAGE);
    }

    try {
      Path name = Paths.get(filename.replace('\\', '/')).normalize().getFileName();
      if (name == null) {
        throw new IllegalArgumentException(INVALID_FILENAME_MESSAGE);
      }

      String result = name.toString();
      if (result.isEmpty() || result.equals(".") || result.equals("..")) {
        throw new IllegalArgumentException(INVALID_FILENAME_MESSAGE);
      }
      return result;
    } catch (InvalidPathException e) {
      throw new IllegalArgumentException(INVALID_FILENAME_MESSAGE, e);
    }
  }
}
