/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

// This is a naive way to make sbt.internal.io.Source accessible. That is why we
// are declaring the package here as sbt.internal.io. You can see the code for
// sbt.internal.io.Source here:
// https://github.com/sbt/io/blob/v1.1.0/io/src/main/scala/sbt/internal/io/SourceModificationWatch.scala#L128-L157
//
// We can eventually discard this if sbt decides to change the API and make it
// public.
/**
 * INTERNAL API: Provides access to sbt.internal.io.Source methods.
 */
package sbt.internal.io {
  class PlaySource(source: sbt.internal.io.Source) {
    def getFiles = {
      source.getUnfilteredPaths
        .filter(p => source.accept(p))
        .map(_.toFile)
    }
  }
}