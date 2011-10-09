package play.core.logger

trait Logging {
    
    def log(msg: String)
    
}

object Logger extends Logging {
    
    def log(msg: String) {
        println("[info] " + msg)
    }
    
}