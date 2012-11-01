package play.libs;

import akka.actor.*;
import akka.actor.Actor.*;
import scala.concurrent.ExecutionContext;
/**
 * provides Play's internal actor system and the corresponding execution context
 */
class Invoker {

   private static final ActorSystem sys = ActorSystem.apply("play");

  /**
   * provides actor system for Java Promises
   */
  static ActorSystem system() {return sys;}
  
 /**
  * provides execution context for Java Promises
  */ 
  static ExecutionContext executionContext() {return sys.dispatcher();}
 

}
