/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import org.apache.pekko.annotation.ApiMayChange
import org.apache.pekko.stream.Materializer
import org.openqa.selenium.WebDriver
import org.specs2.execute.AsResult
import org.specs2.execute.Result
import org.specs2.execute.Success
import org.specs2.mutable.Around
import org.specs2.specification.ForEach
import org.specs2.specification.Scope
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.guice.GuiceApplicationLoader
import play.api.Application
import play.api.ApplicationLoader
import play.api.Environment
import play.api.Mode
import play.core.server.ServerProvider

abstract class AroundHelper(helperClass: Class[?]) extends Around {

  private var directlyCallWrap             = false
  private var scala2RunningAlreadyExecuted = false

  def running(): Unit = throw new NotImplementedError(
    s"""
       |For Scala 3 you need to wrap the body of ${helperClass.getSimpleName} in an `override def running() = ...` method:
       |
       |// Old:
       |new ${helperClass.getSimpleName}() {
       |  <code>
       |}
       |
       |// New:
       |new ${helperClass.getSimpleName}() {
       |  override def running() = {
       |    <code>
       |  }
       |}
       |""".stripMargin
  )

  // This initialization code here is the entry point in Scala 3:
  // When running Scala 3, the Around.delayedInit() method (which calls the below around() method) will NOT run.
  // Therefore we have to call and execute that method here manually
  directlyCallWrap = true
  super.delayedInit(
    // We simulate the Scala 2 behaviour by calling delayedInit/around manually, with the code from the running method
    running()
  )

  def wrap[T: AsResult](t: => T): Result

  override def around[T: AsResult](t: => T): Result = {
    // This is the entry point in Scala 2 (in addition being also called by Scala 3 manually above)
    // When running Scala 2, this around method, which gets called by Around.delayedInit(), will run either once or twice:
    // Scala 2 will always run it for the initialization code of this abstract class (being the init code above where we call delayedInit manually for Scala 3)
    // It may also run a second time if a concrete instance contains initialization code as well (e.g. '"..." in new WithServer(...) { <initialization code> }')
    if (directlyCallWrap) {
      // This if branch will be entered when manually calling delayedInit in Scala 3 (see "entry point" for Scala 3 above)
      // Or in Scala 2, on the second run of the around method, when init code of a concrete instance should be executed
      // (and if there is no running method defined in that concrete instance)
      wrap(t)
    } else {
      // Scala 3 will never enter this else branch, it always directly calls wrap
      // Scala 2 will always enter this else branch on the first run of around/delayedInit
      if (this.getClass.getMethod("running").getDeclaringClass != classOf[AroundHelper]) {
        // In Scala 2 a running method was detected:  '"..." in new WithServer(...) { override def running() = { <to-be-tested> } }'
        // That can happen when switching back from Scala 3 to Scala 2 or while migrating to Scala 3 but still compiling/testing with Scala 2.
        // Therefore here we execute the running method, but we do not set directlyCallWrap to true so even when this around method would run a second time
        // (entering this else branch here again) because a concrete instance has initialization code defined, such initialization code wouldn't be wrapped.
        if (!scala2RunningAlreadyExecuted) {
          scala2RunningAlreadyExecuted = true
          wrap(Result.resultOrSuccess(running()).asInstanceOf[T])
        } else {
          // If the concrete class has init code, there would be a second run of around/delayedInit in which we would end up in this else branch.
          // To stay on par with Scala 3 we also execute such init code now (Scala 3 does the same, first runs init code from abstract class then from concrete instance)
          t
          Success()
        }
      } else {
        // In Scala 2 no running method was detected, so if the concrete class has init code, that code should be executed on the second run of around/delayedInit
        // (BTW: If there would be no init code, then there will be no second run and the test would just be successful, which is the same behaviour like in Play 2.8)
        directlyCallWrap = true
        Success()
      }
    }
  }
}

// NOTE: Do *not* put any initialisation code in the below classes, otherwise delayedInit() gets invoked twice
// which means around() gets invoked twice and everything is not happy.  Only lazy vals and defs are allowed, no vals
// or any other code blocks.

/**
 * Used to run specs within the context of a running application loaded by the given `ApplicationLoader`.
 *
 * @param applicationLoader The application loader to use
 * @param context The context supplied to the application loader
 */
abstract class WithApplicationLoader(
    applicationLoader: ApplicationLoader = new GuiceApplicationLoader(),
    context: ApplicationLoader.Context = ApplicationLoader.Context.create(
      new Environment(new java.io.File("."), ApplicationLoader.getClass.getClassLoader, Mode.Test)
    )
) extends AroundHelper(classOf[WithApplicationLoader])
    with Scope {
  implicit lazy val app: Application              = applicationLoader.load(context)
  override def wrap[T: AsResult](t: => T): Result = {
    Helpers.running(app)(AsResult.effectively(t))
  }
}

/**
 * Used to run specs within the context of a running application.
 *
 * @param app The fake application
 */
abstract class WithApplication(val app: Application = GuiceApplicationBuilder().build())
    extends AroundHelper(classOf[WithApplication])
    with Scope {
  def this(builder: GuiceApplicationBuilder => GuiceApplicationBuilder) = {
    this(builder(GuiceApplicationBuilder()).build())
  }

  implicit def implicitApp: Application           = app
  implicit def implicitMaterializer: Materializer = app.materializer
  override def wrap[T: AsResult](t: => T): Result = {
    Helpers.running(app)(AsResult.effectively(t))
  }
}

/**
 * Used to run specs within the context of a running server.
 *
 * @param app The fake application
 * @param port The port to run the server on
 * @param serverProvider *Experimental API; subject to change* The type of
 * server to use. Defaults to providing a Netty server.
 */
abstract class WithServer(
    val app: Application = GuiceApplicationBuilder().build(),
    var port: Int = Helpers.testServerPort,
    val serverProvider: Option[ServerProvider] = None
) extends AroundHelper(classOf[WithServer])
    with Scope {
  implicit def implicitMaterializer: Materializer = app.materializer
  implicit def implicitApp: Application           = app
  implicit def implicitPort: Port                 = port

  override def wrap[T: AsResult](t: => T): Result = {
    val currentPort = port
    val result      = Helpers.runningWithPort(TestServer(port = port, application = app, serverProvider = serverProvider)) {
      assignedPort =>
        port = assignedPort // if port was 0, the OS assigns a random port
        AsResult.effectively(t)
    }
    port = currentPort
    result
  }
}

/** Replacement for [[WithServer]], adding server endpoint info. */
@ApiMayChange trait ForServer extends ForEach[RunningServer] with Scope {
  protected def applicationFactory: ApplicationFactory
  protected def testServerFactory: TestServerFactory = new DefaultTestServerFactory()

  protected final def foreach[R: AsResult](f: RunningServer => R): Result = {
    val app: Application = applicationFactory.create()
    val runningServer    = testServerFactory.start(app)
    try AsResult.effectively(f(runningServer))
    finally runningServer.stopServer.close()
  }
}

/**
 * Used to run specs within the context of a running server, and using a web browser
 *
 * @param webDriver The driver for the web browser to use
 * @param app The fake application
 * @param port The port to run the server on
 */
abstract class WithBrowser[WEBDRIVER <: WebDriver](
    val webDriver: WebDriver = WebDriverFactory(Helpers.HTMLUNIT),
    val app: Application = GuiceApplicationBuilder().build(),
    var port: Int = Helpers.testServerPort
) extends AroundHelper(classOf[WithBrowser[?]])
    with Scope {
  def this(webDriver: Class[WEBDRIVER], app: Application, port: Int) = this(WebDriverFactory(webDriver), app, port)

  implicit def implicitApp: Application = app
  implicit def implicitPort: Port       = port

  override def wrap[T: AsResult](t: => T): Result = {
    var browser: TestBrowser = null
    try {
      val currentPort = port
      val result      = Helpers.runningWithPort(TestServer(port, app)) { assignedPort =>
        port = assignedPort // if port was 0, the OS assigns a random port
        browser = new TestBrowser(webDriver, Some("http://localhost:" + assignedPort))
        AsResult.effectively(t)
      }
      port = currentPort
      result
    } finally {
      Option(browser).foreach(_.quit())
    }
  }
}
