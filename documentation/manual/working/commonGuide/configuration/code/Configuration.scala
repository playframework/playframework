/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
// With Play Scala

object DependencyInjection {
  //#dependency-injection
  import javax.inject._
  import play.api.Configuration

  class MyController @Inject()(config: Configuration) {
    // ...
  }
  //#dependency-injection
}
