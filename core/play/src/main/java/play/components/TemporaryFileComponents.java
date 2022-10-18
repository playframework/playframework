/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import play.libs.Files;

/** Components related to temporary file handle. */
public interface TemporaryFileComponents {

  Files.TemporaryFileCreator tempFileCreator();
}
