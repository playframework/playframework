package play.api.inject.guice

import org.specs2.mutable.Specification

import com.google.inject.AbstractModule

import play.api.ApplicationLoader.Context
import play.api.Configuration
import play.api.{ Mode, Environment }
import play.core.DefaultWebCommands
import play.api.inject.BuiltinModule

class GuiceApplicationLoaderSpec extends Specification {

  "GuiceApplicationLoader" should {
    "allow adding additional modules" in {
      val loader = new GuiceApplicationLoader(new AbstractModule {
        def configure() = {
          bind(classOf[Bar]) to classOf[MarsBar]
        }
      })
      val app = loader.load(fakeContext)
      app.injector.instanceOf[Bar] must beAnInstanceOf[MarsBar]
    }
    "allow replacing existing modules" in {
      val loader = new GuiceApplicationLoader() {
        override def loadModules(env: Environment, conf: Configuration) =
          Seq(guicify(env, conf, new BuiltinModule), new ManualTestModule)
      }
      val app = loader.load(fakeContext)
      app.injector.instanceOf[Foo] must beAnInstanceOf[ManualFoo]
    }
  }

  def fakeContext = Context(
    Environment(new java.io.File("."), getClass.getClassLoader, Mode.Test),
    None,
    new DefaultWebCommands,
    Configuration.load(new java.io.File("."), Mode.Test)
  )
}

class ManualTestModule extends AbstractModule {
  def configure(): Unit = {
    bind(classOf[Foo]) to classOf[ManualFoo]
  }
}

trait Bar
class MarsBar extends Bar

trait Foo
class ManualFoo extends Foo
