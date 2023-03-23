/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scaladoc {
  package mycomponent {

//#components-decl
    import javax.inject.Inject

    import scala.concurrent.Future

    import play.api.inject.ApplicationLifecycle

    trait MyComponent

    class MyComponentImpl @Inject() (lifecycle: ApplicationLifecycle) extends MyComponent {
      // previous contents of Plugin.onStart
      lifecycle.addStopHook { () =>
        // previous contents of Plugin.onStop
        Future.successful(())
      }
    }
//#components-decl
  }
}
