package play.api.db.slick

import play.api.Application
import play.api.Plugin
import play.libs.ReflectionsCache
import org.reflections.scanners.TypesScanner
import scala.slick.session.Database
import play.api.libs.Files
import play.api.Mode
import scala.slick.lifted.DDL
import play.api.PlayException

object ReflectionUtils {
  import annotation.tailrec
  import scala.reflect.runtime.universe
  import scala.reflect.runtime.universe._

  def splitIdentifiers(names: String) = names.split("""\.""").filter(!_.trim.isEmpty).toList
  def assembleIdentifiers(ids: List[String]) = ids.mkString(".")

  def findFirstModule(names: String)(implicit mirror: JavaMirror): Option[ModuleSymbol] = {
    val elems = splitIdentifiers(names)
    var i = 1 //FIXME: vars...
    var res: Option[ModuleSymbol] = None
    while (i < (elems.size + 1) && !res.isDefined) {
      try {
        res = Some(mirror.staticModule(assembleIdentifiers(elems.slice(0, i))))
      } catch {
        case e: reflect.internal.MissingRequirementError =>
        //FIXME: must be another way to check if a static modules exists than exceptions!?!
      } finally {
        i += 1
      }
    }
    res
  }

  def reflectModuleOrField(name: String, base: Any, baseSymbol: Symbol)(implicit mirror: JavaMirror) = {
    val baseIM = mirror.reflect(base)
    val baseMember = baseSymbol.typeSignature.member(newTermName(name))
    val instance = if (baseMember.isModule) {
      if (baseMember.isStatic) {
        mirror.reflectModule(baseMember.asModule).instance
      } else {
        baseIM.reflectModule(baseMember.asModule).instance
      }
    } else {
      assert(baseMember.isTerm, "Expected " + baseMember + " to be something that can be reflected on " + base + " as a field")
      baseIM.reflectField(baseMember.asTerm).get
    }
    instance -> baseMember
  }

  def scanModuleOrFieldByReflection(instance: Any, sym: Symbol)(checkSymbol: Symbol => Boolean)(implicit mirror: JavaMirror): List[(Any, Symbol)] = {
    @tailrec def scanModuleOrFieldByReflection(found: List[(Any, Symbol)],
      checked: Vector[Symbol],
      instancesNsyms: List[(Any, Symbol)]): List[(Any, Symbol)] = {

      val extractMembers: PartialFunction[(Any, Symbol), Iterable[(Any, Symbol)]] = {
        case (baseInstance, baseSym) =>
          if (baseInstance != null) {
            baseSym.typeSignature.members.filter(s => s.isModule || (s.isTerm && s.asTerm.isVal)).map { mSym =>
              reflectModuleOrField(mSym.name.decoded, baseInstance, baseSym)
            }
          } else List.empty
      }
      val matching = instancesNsyms.flatMap(extractMembers).filter { case (_, s) => checkSymbol(s) }
      val candidates = instancesNsyms.flatMap(extractMembers).filter { case (_, s) => !checkSymbol(s) && !checked.contains(s) }
      if (candidates.isEmpty)
        (found ++ matching).distinct
      else
        scanModuleOrFieldByReflection(found ++ matching, checked ++ (matching ++ candidates).map(_._2), candidates)
    }

    scanModuleOrFieldByReflection(List.empty, Vector.empty, List(instance -> sym))
  }

}

class SlickDDLPlugin(app: Application) extends Plugin {
  private val configKey = "slick"

  private def isDisabled: Boolean = app.configuration.getString("evolutionplugin").map(_ == "disabled").headOption.getOrElse(false)

  override def enabled = !isDisabled

  class SlickDDLException(val message: String) extends Exception(message)

  override def onStart(): Unit = {
    val conf = app.configuration.getConfig(configKey)
    conf.foreach { conf =>
      conf.keys.foreach { key =>
        val packageNames = conf.getString(key).getOrElse(throw conf.reportError(key, "Expected key " + key + " but could not get its values!", None)).split(",").toSet
        if (app.mode != Mode.Prod) {
          val evolutionsEnabled = !"disabled".equals(app.configuration.getString("evolutionplugin"))
          if (evolutionsEnabled) {
            val evolutions = app.getFile("conf/evolutions/" + key + "/1.sql");
            if (!evolutions.exists() || Files.readFile(evolutions).startsWith(CreatedBy)) {
              try {
                evolutionScript(packageNames).foreach { evolutionScript =>
                  Files.createDirectory(app.getFile("conf/evolutions/" + key));
                  Files.writeFileIfChanged(evolutions, evolutionScript);
                }
              } catch {
                case e: SlickDDLException => throw conf.reportError(key, e.message, Some(e))
              }
            }
          }
        }
      }
    }
  }

  private val CreatedBy = "# --- Created by "

  private val WildcardPattern = """(.*)\.\*""".r

  def evolutionScript(names: Set[String]): Option[String] = {
    val classloader = app.classloader

    import scala.collection.JavaConverters._

    val ddls = reflectAllDDLMethods(names, classloader)

    val delimiter = ";" //TODO: figure this out by asking the db or have a configuration setting?

    if (ddls.nonEmpty) {
      val ddl = ddls.reduceLeft(_ ++ _)

      Some(CreatedBy + "Slick DDL\n" +
        "# To stop Slick DDL generation, remove this comment and start using Evolutions\n" +
        "\n" +
        "# --- !Ups\n\n" +
        ddl.createStatements.mkString("", s"$delimiter\n", s"$delimiter\n") +
        "\n" +
        "# --- !Downs\n\n" +
        ddl.dropStatements.mkString("", s"$delimiter\n", s"$delimiter\n") +
        "\n")
    } else None
  }

  def reflectAllDDLMethods(names: Set[String], classloader: ClassLoader): Seq[DDL] = {
    import scala.reflect.runtime.universe
    import scala.reflect.runtime.universe._

    implicit val mirror = universe.runtimeMirror(classloader)

    val tableType = typeOf[slick.driver.BasicTableComponent#Table[_]]
    def isTable(sym: Symbol) = {
      sym.typeSignature.baseClasses.find(_.typeSignature == tableType.typeSymbol.typeSignature).isDefined
    }
    def tableToDDL(instance: Any) = {
      import scala.language.reflectiveCalls //this is the simplest way to do achieve this, we are using reflection either way...
      instance.getClass -> instance.asInstanceOf[{ def ddl: DDL }].ddl
    }

    val classesAndNames = names.flatMap { name =>
      ReflectionUtils.findFirstModule(name) match {
        case Some(baseSym) => { //located a module that matches, reflect each module/field in the name then scan for Tables
          val baseInstance = mirror.reflectModule(baseSym).instance

          val allIds = ReflectionUtils.splitIdentifiers(name.replace(baseSym.fullName, ""))
          val isWildCard = allIds.lastOption.map(_ == "*").getOrElse(false)
          val ids = if (isWildCard) allIds.init else allIds

          val (outerInstance, outerSym) = ids.foldLeft(baseInstance -> (baseSym: Symbol)) {
            case ((instance, sym), id) =>
              ReflectionUtils.reflectModuleOrField(id, instance, sym)
          }

          val foundInstances = if (isTable(outerSym) && !isWildCard) {
            List(outerInstance)
          } else if (isWildCard) {
            val instancesNsyms = ReflectionUtils.scanModuleOrFieldByReflection(outerInstance, outerSym)(isTable)
            if (instancesNsyms.isEmpty) play.api.Logger.warn("Scanned object: '" + baseSym.fullName + "' for '" + name + "' but did not find any Slick Tables")
            instancesNsyms.map(_._1)
          } else {
            throw new SlickDDLException("Found a matching object: '" + baseSym.fullName + "' for '" + name + "' but it is not a Slick Table and a wildcard was not specified")
          }
          foundInstances.map { instance => tableToDDL(instance) }
        }
        case _ => { //no modules located, scan packages..
          import scala.collection.JavaConverters._
          val classNames = name match {
            case WildcardPattern(p) => ReflectionsCache.getReflections(classloader, p) //TODO: would be nicer if we did this using Scala reflection, alas staticPackage is non-deterministic:  https://issues.scala-lang.org/browse/SI-6573
              .getStore
              .get(classOf[TypesScanner])
              .keySet.asScala.toSet
            case p => Set(p)
          }
          classNames.flatMap { className =>

            val moduleSymbol = try { //FIXME: ideally we should be able to test for existence not use exceptions
              Some(mirror.staticModule(className))
            } catch {
              case e: scala.reflect.internal.MissingRequirementError => None
            }

            moduleSymbol.filter(isTable).map { moduleSymbol =>
              tableToDDL(mirror.reflectModule(moduleSymbol).instance)
            }
          }
        }
      }
    }

    classesAndNames.toSeq.sortBy(_._1.toString).map(_._2).distinct
  }
}
