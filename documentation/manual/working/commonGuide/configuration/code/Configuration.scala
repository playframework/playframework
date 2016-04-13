// With Play Scala

object DependencyInjection {
  //#dependency-injection
  import javax.inject._
  import play.api.Configuration

  class MyController @Inject() (config: Configuration) {
    // ...
  }
  //#dependency-injection
}
