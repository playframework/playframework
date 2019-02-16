/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scaladoc {
  package mycomponent {

//#components-decl
    import javax.inject.Inject
    import play.api.inject.ApplicationLifecycle
    import scala.concurrent.Future

    trait MyComponent

    class MyComponentImpl @Inject()(lifecycle: ApplicationLifecycle) extends MyComponent {
      // previous contents of Plugin.onStart
      lifecycle.addStopHook { () =>
        // previous contents of Plugin.onStop
        Future.successful(())
      }
    }
//#components-decl
  }
}
