package play.core

import akka.actor.Actor

class AkkaListener extends Actor {
    
    def receive = {
        case e => println(e)
    }
    
}