package play.api.libs

import scalax.io._
import scalax.file._

import java.io._

object Files {
    
    def readFile(path:File):String = Path(path).slurpString
    
    def writeFile(path:File, content:String) = Path(path).write(content)
    
    def createDirectory(path:File) = Path(path).createDirectory(failIfExists=false)
    
    def writeFileIfChanged(path:File, content:String) {
        if(content != Option(path).filter(_.exists).map(readFile(_)).getOrElse("")) {
            writeFile(path, content)
        }
    }
    
}