package play.forkrunner

import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.net.URL
import java.util.Enumeration
import java.util.Vector
import play.core.classloader.ApplicationClassLoaderProvider
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

object DelegatingClassLoader {
  val buildSharedClasses =
    List[String](
      classOf[play.core.BuildLink].getName(),
      classOf[play.core.BuildDocHandler].getName(),
      classOf[play.core.server.ServerWithStop].getName(),
      classOf[play.api.UsefulException].getName(),
      classOf[play.api.PlayException].getName(),
      classOf[play.api.PlayException.InterestingLines].getName(),
      classOf[play.api.PlayException.RichDescription].getName(),
      classOf[play.api.PlayException.ExceptionSource].getName(),
      classOf[play.api.PlayException.ExceptionAttachment].getName(),
      classOf[play.runsupport.ForkRunnerException].getName(),
      classOf[play.runsupport.PlayExceptionNoSource].getName(),
      classOf[play.runsupport.PlayExceptionWithSource].getName(),
      classOf[xsbti.Severity].getName())

  def isSharedClass(name: String): Boolean =
    buildSharedClasses.contains(name) || name.startsWith("com.typesafe.config") || name.startsWith("java.") || name.startsWith("scala.")

  private def combineResources(resources1: Enumeration[URL], resources2: Enumeration[URL]): Enumeration[URL] = {
    (resources1.toSet ++ resources2.toSet).toIterator
  }

}

class DelegatingClassLoader(commonLoader: ClassLoader, buildLoader: ClassLoader, applicationClassLoaderProvider: ApplicationClassLoaderProvider) extends ClassLoader(commonLoader) {
  import DelegatingClassLoader._

  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    if (isSharedClass(name)) buildLoader.loadClass(name)
    else super.loadClass(name, resolve)
  }

  override def getResource(name: String): URL = {
    // -- Delegate resource loading. We have to hack here because the default implementation is already recursive.
    val findResource: Method = try { classOf[ClassLoader].getDeclaredMethod("findResource", classOf[String]) }
    catch { case e: NoSuchMethodException => throw new IllegalStateException(e) }
    findResource.setAccessible(true)
    try {
      (for {
        appClassLoader <- Option(applicationClassLoaderProvider.get())
        resource <- Option(findResource.invoke(appClassLoader, name).asInstanceOf[URL])
      } yield resource) getOrElse super.getResource(name)
    } catch {
      case e: IllegalAccessException => throw new IllegalStateException(e)
      case e: InvocationTargetException => throw new IllegalStateException(e)
    }
  }

  override def getResources(name: String): Enumeration[URL] = {
    val findResources: Method = try { classOf[ClassLoader].getDeclaredMethod("findResources", classOf[String]) }
    catch { case e: NoSuchMethodException => throw new IllegalStateException(e) }
    findResources.setAccessible(true)
    val resources1: Enumeration[URL] = try {
      (for {
        appClassLoader <- Option(applicationClassLoaderProvider.get())
        resources <- Option(findResources.invoke(appClassLoader, name).asInstanceOf[Enumeration[URL]])
      } yield resources) getOrElse (new Vector[URL]().elements)
    } catch {
      case e: IllegalAccessException => throw new IllegalStateException(e)
      case e: InvocationTargetException => throw new IllegalStateException(e)
    }
    val resources2: Enumeration[URL] = super.getResources(name)
    combineResources(resources1, resources2)
  }

  override def toString(): String = {
    return "DelegatingClassLoader, using parent: " + getParent()
  }
}
