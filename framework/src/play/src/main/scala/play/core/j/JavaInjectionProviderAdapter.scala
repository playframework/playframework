package play.core.j

import scala.collection.JavaConverters._
import scala.util.Try

import play.api.{ GlobalSettings, Plugin, InjectionProvider }

class JavaInjectionProviderAdapter(javaProvider: play.InjectionProvider) extends InjectionProvider {

  def plugins: Seq[Plugin] = javaProvider.plugins().asScala

  def getInstance[T](clazz: Class[T]): Try[T] = Try(javaProvider.getInstance(clazz))

  def getPlugin[T <: Plugin](clazz: Class[T]): Option[T] = Option(javaProvider.getPlugin(clazz))

  def global: GlobalSettings = new JavaGlobalSettingsAdapter(javaProvider.global)
}
