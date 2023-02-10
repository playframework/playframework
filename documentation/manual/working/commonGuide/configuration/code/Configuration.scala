/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

// With Play Scala

object DependencyInjection {
  // #dependency-injection
  import javax.inject._

  import play.api.Configuration

  class MyController @Inject() (config: Configuration) {
    // ...
  }
  // #dependency-injection
}
