package play.api

import play.core._

import java.io._

import scala.util.parsing.input._
import scala.util.parsing.combinator._
import scala.util.matching._

object Configuration {
    
    def fromFile(file:File) = {
        Configuration(
            new ConfigurationParser(file).parse.map(c => c.key -> c).toMap
        )
    }
    
    class ConfigurationParser(configurationFile:File) extends RegexParsers {
        
        case class Comment(msg:String)
        
        override def skipWhitespace = false
        override val whiteSpace = """[ \t]+""".r

        override def phrase[T](p: Parser[T]) = new Parser[T] {
            lastNoSuccess = null
            def apply(in: Input) = p(in) match {
                case s @ Success(out, in1) =>
                    if (in1.atEnd) 
                        s
                    else if (lastNoSuccess == null || lastNoSuccess.next.pos < in1.pos)
                        Failure("end of input expected", in1)
                    else 
                        lastNoSuccess
                case _ => lastNoSuccess
            }
        }
        
        def namedError[A](p:Parser[A], msg:String) = Parser[A] { i =>
            p(i) match {
                case Failure(_, in) => Failure(msg, in)
                case o => o
            }
        }
        
        def end = """\s*""".r
        def newLine = namedError("\n", "End of line expected")
        def blankLine = ignoreWhiteSpace <~ newLine ^^ {case _ => Comment("")}
        def ignoreWhiteSpace = opt(whiteSpace)
        
        def comment = """#.*""".r ^^ {case s => Comment(s)}
        
        def configKey = namedError("""[a-zA-Z0-9_.]+""".r, "Configuration key expected")
        def configValue = namedError(""".+""".r, "Configuration value expected")
        def config = ignoreWhiteSpace ~ configKey ~ (ignoreWhiteSpace ~ "=" ~ ignoreWhiteSpace) ~ configValue ^^ {
            case (_~k~_~v) => Config(k,v.trim,configurationFile)
        }
        
        def sentence = (comment | positioned(config)) <~ newLine
        
        def parser = phrase( (sentence | blankLine *) <~ end ) ^^ {
            case configs => configs.collect {
                case c@Config(_,_,_) => c
            }
        }
        
        def parse = {
            parser(new CharSequenceReader(scalax.file.Path(configurationFile).slurpString)) match {
                case Success(configs, _) => configs
                case NoSuccess(message, in) => {
                    throw new PlayException("Configuration error", message) with ExceptionSource {
                        def line = Some(in.pos.line)
                        def position = Some(in.pos.column-1)
                        def file = Some(configurationFile)
                    }
                }
            }
        }
        
    }
    
}

case class Config(key:String, value:String, file:File) extends Positional

case class Configuration(data:Map[String,Config], root:String = "") {
    
    def get(key:String):Option[Config] = data.get(key)
    
    def getString(key:String, validValues:Option[Set[String]] = None):Option[String] = data.get(key).map { c =>
        validValues match {
            case Some(values) if values.contains(c.value) => c.value
            case Some(values) if values.isEmpty => c.value
            case Some(values) => throw error("Incorrect value, one of " + (values.reduceLeft(_ + ", " + _)) + " was expected.", c)
            case None => c.value
        }
    }
    
    def getInt(key:String):Option[Int] = data.get(key).map { c =>
        try {
            Integer.parseInt(c.value)
        } catch {
            case e => throw error("Integer value required", c)
        }
    }
    
    def getBoolean(key:String):Option[Boolean] = data.get(key).map { c =>
        c.value match {
            case "true" => true
            case "yes" => true
            case "false" => false
            case "no" => false
            case o => throw error("Boolean value required", c)
        }
    }
    
    def getSub(key:String):Option[Configuration] = Option(data.filterKeys(_.startsWith(key+".")).map {
        case (k, c) => k.drop(key.size + 1) -> c
    }.toMap).filterNot(_.isEmpty).map(Configuration(_, full(key) + "."))
    
    def sub(key:String):Configuration = getSub(key).getOrElse {
        throw globalError("No configuration found '" + key + "'") 
    }
    
    def keys:Set[String] = data.keySet
    
    def subKeys:Set[String] = keys.map(_.split('.').head)
    
    def reportError(key:String, message:String, e:Option[Throwable] = None) = {
        data.get(key).map { config =>
            error(message, config, e)
        }.getOrElse {
            new PlayException("Configuration error", full(key) + ": " + message, e)
        }
    }
    
    def full(key:String) = root + key
    
    def globalError(message:String, e:Option[Throwable] = None) = {
        data.headOption.map { c =>
            new PlayException("Configuration error", message, e) with ExceptionSource {
                def line = Some(c._2.pos.line)
                def position = None
                def file = Some(c._2.file)
            }
        }.getOrElse {
            new PlayException("Configuration error", message, e)
        }
    }
    
    private def error(message:String, config:Config, e:Option[Throwable] = None) = {
        new PlayException("Configuration error", message, e) with ExceptionSource {
            def line = Some(config.pos.line)
            def position = Some(config.pos.column + config.key.size)
            def file = Some(config.file)
        } 
    }
    
}