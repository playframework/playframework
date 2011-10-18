package play.api

import play.core._

import play.api.mvc._

import java.io._

import scala.collection.JavaConverters._

case class Application(path: File, classloader: ApplicationClassLoader, sources: SourceMapper, mode: Play.Mode.Mode) {

  val global: GlobalSettings = try {
    classloader.loadClassParentLast("Global$").getDeclaredField("MODULE$").get(null).asInstanceOf[GlobalSettings]
  } catch {
    case e: ClassNotFoundException => DefaultGlobal
    case e => throw e
  }

  global.beforeStart(this)

  val routes: Option[Router.Routes] = try {
    Some(classloader.loadClassParentLast("Routes$").getDeclaredField("MODULE$").get(null).asInstanceOf[Router.Routes])
  } catch {
    case e: ClassNotFoundException => None
    case e => throw e
  }

  val configuration = Configuration.fromFile(new File(path, "conf/application.conf"))

  val plugins: Map[Class[_], Plugin] = {

    import scalax.file._
    import scalax.io.JavaConverters._
    import scala.collection.JavaConverters._

    val PluginDeclaration = """([0-9_]+):(.*)""".r

    classloader.getResources("play.plugins").asScala.toList.distinct.map { plugins =>
      plugins.asInput.slurpString.split("\n").map(_.trim).filterNot(_.isEmpty).map {
        case PluginDeclaration(priority, className) => {
          try {
            Integer.parseInt(priority) -> classloader.loadClass(className).getConstructor(classOf[Application]).newInstance(this).asInstanceOf[Plugin]
          } catch {
            case e: java.lang.NoSuchMethodException => {
              try {
                Integer.parseInt(priority) -> classloader.loadClass(className).getConstructor(classOf[play.Application]).newInstance(new play.Application(this)).asInstanceOf[Plugin]
              } catch {
                case e => throw PlayException(
                  "Cannot load plugin",
                  "Plugin [" + className + "] cannot been instantiated.",
                  Some(e))
              }
            }
            case e => throw PlayException(
              "Cannot load plugin",
              "Plugin [" + className + "] cannot been instantiated.",
              Some(e))
          }
        }
      }
    }.flatten.toList.sortBy(_._1).map(_._2).map(p => p.getClass -> p).toMap

  }

  def plugin[T](implicit m: Manifest[T]): T = plugin(m.erasure).asInstanceOf[T]
  def plugin[T](c: Class[T]): T = plugins.get(c).get.asInstanceOf[T]

  def getFile(subPath: String) = new File(path, subPath)

  def getTypesAnnotatedWith[T <: java.lang.annotation.Annotation](packageName: String, annotation: Class[T]): Set[String] = {
    import org.reflections._
    new Reflections(
      new util.ConfigurationBuilder()
        .addUrls(util.ClasspathHelper.forPackage(packageName, classloader))
        .setScanners(new scanners.TypeAnnotationsScanner())).getStore.getTypesAnnotatedWith(annotation.getName).asScala.toSet
  }

}