/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package sbt
package internal.io

import java.nio.file.Path

// This is a naive way to make sbt.internal.io.Source accessible. That is why we
// are declaring the package here as sbt.internal.io. You can see the code for
// sbt.internal.io.Source here:
// https://github.com/sbt/io/blob/v1.1.0/io/src/main/scala/sbt/internal/io/SourceModificationWatch.scala#L128-L157
//
// We can eventually discard this if sbt decides to change the API and make it public.

/**
 * INTERNAL API: Provides access to sbt.internal.io.Source methods.
 */
class PlaySource(source: Source) {
  def getPaths: Seq[Path] = source.getUnfilteredPaths.filter(source.accept(_))
}
