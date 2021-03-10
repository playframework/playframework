/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package sbt.internal

// These file is a hack:
// In sbt 0.13.x there is no "sbt.internal" package...
// ... but we need one so we can "import sbt.internal._" in build.sbt

object FooBar { }
