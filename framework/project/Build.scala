import sbt._
import Keys._

object PlayBuild extends Build {
    
    import Resolvers._
    import Dependencies._
    import BuildSettings._
    import Generators._
    import LocalSBT._
    import Tasks._
    
    val TemplatesProject = Project(
        "Templates",
        file("templates"),
        settings = buildSettings ++ Seq(
            libraryDependencies := templates,
            publishMavenStyle := false,
            publishTo := Some(playRepository),
            publishArtifact in (Compile, packageDoc) := false,
            publishArtifact in (Compile, packageSrc) := false,
            resolvers += typesafe
        )
    )

    val PlayProject = Project(
        "Play",
        file("play"),
        settings = buildSettings ++ Seq(
            libraryDependencies := runtime,
            sourceGenerators in Compile <+= sourceManaged in Compile map PlayVersion,
            unmanagedJars in Compile ++= sbtJars,
            publishMavenStyle := false,
            publishTo := Some(playRepository),
            publishArtifact in (Compile, packageDoc) := false,
            publishArtifact in (Compile, packageSrc) := false,
            resolvers ++= Seq(typesafe, akkaRepo),
            sourceGenerators in Compile <+= (dependencyClasspath in TemplatesProject in Runtime, packageBin in TemplatesProject in Compile, scalaSource in Compile, sourceManaged in Compile) map ScalaTemplates
        )
    ).dependsOn(TemplatesProject)
    
    val Root = Project(
        "Root",
        file("."),
        settings = buildSettings ++ Seq(
            cleanFiles ++= Seq(file("../dist"), file("../repository")),
            resetRepositoryTask,
            buildRepositoryTask,
            distTask,
            publish <<= (publish in PlayProject, publish in TemplatesProject) map { (_,_) => }
        )
    ).dependsOn(PlayProject).aggregate(TemplatesProject, PlayProject)
    
    object BuildSettings {

        val buildOrganization = "play"
        val buildVersion      = "2.0"
        val buildScalaVersion = "2.9.0"
        val buildSbtVersion   = "0.10.1"

        val buildSettings = Defaults.defaultSettings ++ Seq (
            organization   := buildOrganization,
            version        := buildVersion,
            scalaVersion   := buildScalaVersion
        )

    }

    object LocalSBT {

        import BuildSettings._

        def isJar(f:java.io.File) = f.getName.endsWith(".jar")

        val sbtJars = {
            file("sbt/boot/scala-" + buildScalaVersion + "/org.scala-tools.sbt/sbt/" + buildSbtVersion).listFiles.filter(isJar) ++
            file("sbt/boot/scala-" + buildScalaVersion + "/org.scala-tools.sbt/sbt/" + buildSbtVersion + "/xsbti").listFiles.filter(isJar) ++
            Seq(file("sbt/boot/scala-" + buildScalaVersion + "/lib/jline.jar"))
        }.map(jar => Attributed.blank(jar.getAbsoluteFile))

    }

    object Resolvers {  

        val playRepository = Resolver.file("Play Local Repository", file("../repository"))(Resolver.ivyStylePatterns)    
        val typesafe = Resolver.url("Typesafe Repository", url("http://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)
        val akkaRepo = "Akka Repo" at "http://akka.io/repository"

    }

    object Dependencies {

        val runtime = Seq(
            "org.jboss.netty"                   %   "netty"             %   "3.2.4.Final",
            "org.slf4j"                         %   "slf4j-api"         %   "1.6.1",
            "com.github.scala-incubator.io"     %%  "file"              %   "0.1.2",
            "se.scalablesolutions.akka"         %   "akka-actor"        %   "1.1.3",
            "org.avaje"                         %   "ebean"             %   "2.7.1",
            "com.h2database"                    %   "h2"                %   "1.3.158",
            "org.scala-tools"                   %%   "scala-stm"         %   "0.3"
        )

        val templates = Seq(
            "com.github.scala-incubator.io"     %%  "file"              %   "0.1.2",
            "org.scala-lang"                    %   "scala-compiler"    %   buildScalaVersion
        )

    }

    object Generators {

        val PlayVersion = { dir:File =>
            val file = dir / "PlayVersion.scala"
            IO.write(file, 
                """|package play.core
                   |
                   |object PlayVersion {
                   |    val current = "%s"       
                   |}
                """.stripMargin.format(BuildSettings.buildVersion)
            )
            Seq(file)
        }

    }

    object Tasks {

        import BuildSettings._

        // ----- Reset repo

        val resetRepository = TaskKey[File]("reset-repository")
        val resetRepositoryTask = resetRepository := {
            val repository = file("../repository")
            IO.delete(repository)
            IO.createDirectory(repository)
            repository
        }

        // ----- Build repo

        val buildRepository = TaskKey[Unit]("build-repository")
        val buildRepositoryTask = TaskKey[Unit]("build-repository") <<= (resetRepository, publish, dependencyClasspath in Runtime, sbtVersion) map { (repository, published, classpath, sbtVersion) =>

            def checksum(algo:String)(bytes:Array[Byte]) = {
                import java.security.MessageDigest
                val digest = MessageDigest.getInstance(algo)
                digest.reset()
                digest.update(bytes)
                digest.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft(""){_ + _}
            }

            def copyWithChecksums(files:(File,File)) {
                IO.copyFile(files._1, files._2)
                Seq("md5","sha1").foreach { algo =>
                    IO.write(file(files._2.getAbsolutePath + "." + algo), checksum(algo)(IO.readBytes(files._2)))
                }
            }

            def writeWithChecksums(f:File, content:String) {
                IO.write(f, content)
                Seq("md5","sha1").foreach { algo =>
                    IO.write(file(f.getAbsolutePath + "." + algo), checksum(algo)(content.getBytes))
                }
            }

            val dependencies = classpath.map(_.data).filter(_.ext == "jar").flatMap { jarFile =>
                val ivyDescriptor = (jarFile.getParentFile.getParentFile * "ivy-*.xml").get.headOption
                ivyDescriptor.map { xmlFile =>
                    val version = xmlFile.getName.drop(4).dropRight(4)
                    val artifactType = jarFile.getParentFile.getName
                    val name = xmlFile.getParentFile.getName
                    val organization = xmlFile.getParentFile.getParentFile.getName
                    (jarFile, artifactType, organization, name, version, xmlFile)
                }
            }

            dependencies.foreach { dep =>

                val depDirectory = repository / dep._3 / dep._4 / dep._5 
                val artifactDir = depDirectory / dep._2
                val ivyDir = depDirectory / "ivys"
                val artifact = artifactDir / (dep._4 + ".jar")
                val ivy = ivyDir / "ivy.xml"

                Seq(artifactDir, ivyDir).foreach(IO.createDirectory)
                Seq(dep._1 -> artifact, dep._6 -> ivy).foreach(copyWithChecksums)
            }

            val scalaIvys = repository / "org.scala-lang" / "scala-library" / buildScalaVersion / "ivys"
            IO.createDirectory(scalaIvys)
            writeWithChecksums(scalaIvys / "ivy.xml",
                """|<?xml version="1.0" encoding="UTF-8"?>
                   |<ivy-module version="2.0">
                   |	<info organisation="org.scala-lang"
                   |		module="scala-library"
                   |		revision="%s"
                   |		status="release"
                   |		publication="20101109190151"
                   |	/>
                   |</ivy-module>
                """.stripMargin.trim.format(buildScalaVersion)
            )

            IO.write(file("../play"),
                """
                    |if [ -f conf/application.yml ]
                    |then
                    |	`dirname $0`/framework/build play "$@"
                    |else
                    |	java -cp `dirname $0`/framework/sbt/boot/scala-%1$s/lib/*:`dirname $0`/framework/sbt/boot/scala-%1$s/org.scala-tools.sbt/sbt/%3$s/*:`dirname $0`/repository/play/play_%1$s/%2$s/jars/* play.console.Console "$@"
                    |fi
                """.stripMargin.trim.format(buildScalaVersion, buildVersion, sbtVersion)
            )

        }

        // ----- Dist package

        val dist = TaskKey[File]("dist")
        val distTask = dist <<= (buildRepository) map { _ =>

            import sbt.NameFilter._

            val root = file("..")
            val packageName = "play-" + buildVersion

            val files = {
                (root ** "*") --- 
                (root ** "dist") --- 
                (root ** "dist" ** "*") --- 
                (root ** "target") --- 
                (root ** "target" ** "*") --- 
                (root ** ".*") ---
                (root ** ".git" ** "*") ---
                (root ** "dropbox" ** "*") ---
                (root ** "cleanIvyCache") ---
                (root ** "*.lock")
            }
            
            val zipFile = root / "dist" / (packageName + ".zip")

            IO.delete(root / "dist")
            IO.createDirectory(root / "dist")
            IO.zip(files x rebase(root, packageName), zipFile)

            zipFile
        }
        
        // ----- Compile templates
        
        val ScalaTemplates = { (classpath:Seq[Attributed[File]], templateEngine:File, sourceDirectory:File, generatedDir:File) =>
            val classloader = new java.net.URLClassLoader(classpath.map(_.data.toURI.toURL).toArray, this.getClass.getClassLoader)
            val compiler = classloader.loadClass("play.templates.ScalaTemplateCompiler")
            val generatedSource = classloader.loadClass("play.templates.GeneratedSource")
            
            (generatedDir ** "template_*").get.foreach { source =>
                val constructor = generatedSource.getDeclaredConstructor(classOf[java.io.File])
                val sync = generatedSource.getDeclaredMethod("sync")
                val generated = constructor.newInstance(source)
                try {
                    sync.invoke(generated)
                } catch {
                    case e:java.lang.reflect.InvocationTargetException =>{
                        val t = e.getTargetException
                        t.printStackTrace()
                        throw t
                    }
                }
            }
            
            (sourceDirectory ** "*.scala.html").get.foreach { template =>
                val compile = compiler.getDeclaredMethod("compile", classOf[java.io.File], classOf[java.io.File], classOf[java.io.File], classOf[String], classOf[String], classOf[String])
                try {
                    compile.invoke(null, template, sourceDirectory, generatedDir, "play.templates.Html", "play.templates.HtmlFormat", "")
                } catch {
                    case e:java.lang.reflect.InvocationTargetException =>{
                        val t = e.getTargetException
                        t.printStackTrace()
                        throw t
                    }
                }
            }
            
            (generatedDir * "template_*.scala").get.map(_.getAbsoluteFile)
        }
        

    }
    
}
