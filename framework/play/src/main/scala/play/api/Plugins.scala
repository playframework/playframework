package play.api

import play.api.mvc._

trait Plugin {
    
    def onStart {}
    def onStop {}
    
}
