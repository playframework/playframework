package play.api.inject.guice

import org.specs2.mutable.Specification

import com.google.inject.AbstractModule

import play.api.{ ApplicationLoader, Configuration, Environment }
import play.api.inject.BuiltinModule

class GuiceApplicationLoaderSpec extends Specification {

  "GuiceApplicationLoader" should {

    "allow adding additional modules" in {
      val module = new AbstractModule {
        def configure() = {
          bind(classOf[Bar]) to classOf[MarsBar]
        }
      }
      val builder = new GuiceApplicationBuilder().bindings(module)
      val loader = new GuiceApplicationLoader(builder)
      val app = loader.load(fakeContext)
      app.injector.instanceOf[Bar] must beAnInstanceOf[MarsBar]
    }

    "allow replacing automatically loaded modules" in {
      val builder = new GuiceApplicationBuilder().load(new BuiltinModule, new ManualTestModule)
      val loader = new GuiceApplicationLoader(builder)
      val app = loader.load(fakeContext)
      app.injector.instanceOf[Foo] must beAnInstanceOf[ManualFoo]
    }

  }

  def fakeContext = ApplicationLoader.createContext(Environment.simple())
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
