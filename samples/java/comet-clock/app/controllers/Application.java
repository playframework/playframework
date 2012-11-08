package controllers;

import play.*;
import play.mvc.*;
import play.libs.*;
import play.libs.F.*;

import akka.util.*;
import akka.actor.*;

import java.util.*;
import java.text.*;
import scala.concurrent.duration.Duration;

import static java.util.concurrent.TimeUnit.*;

import scala.concurrent.ExecutionContext$;

import views.html.*;

public class Application extends Controller {
    
    final static ActorRef clock = Clock.instance;

    public static Result index() {
        return ok(index.render());
    }
    
    public static Result liveClock() {
        return ok(new Comet("parent.clockChanged") {  
            public void onConnected() {
               clock.tell(this); 
            } 
        });
    }
    
    public static class Clock extends UntypedActor {
        
        static ActorRef instance = Akka.system().actorOf(new Props(Clock.class));
        
        // Send a TICK message every 100 millis
        static {
            Akka.system().scheduler().schedule(
                Duration.Zero(),
                Duration.create(100, MILLISECONDS),
                instance, "TICK",  Akka.system().dispatcher()
            );
        }
        
        //
        
        List<Comet> sockets = new ArrayList<Comet>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH mm ss");
        
        public void onReceive(Object message) {

            // Handle connections
            if(message instanceof Comet) {
                final Comet cometSocket = (Comet)message;
                
                if(sockets.contains(cometSocket)) {
                    
                    // Brower is disconnected
                    sockets.remove(cometSocket);
                    Logger.info("Browser disconnected (" + sockets.size() + " browsers currently connected)");
                    
                } else {
                    
                    // Register disconnected callback 
                    cometSocket.onDisconnected(new Callback0() {
                        public void invoke() {
                            getContext().self().tell(cometSocket);
                        }
                    });
                    
                    // New browser connected
                    sockets.add(cometSocket);
                    Logger.info("New browser connected (" + sockets.size() + " browsers currently connected)");
                    
                }
                
            } 
            
            // Tick, send time to all connected browsers
            if("TICK".equals(message)) {
                
                // Send the current time to all comet sockets
                for(Comet cometSocket: sockets) {
                    cometSocket.sendMessage(dateFormat.format(new Date()));
                }
                
            }

        }
        
    }
  
}
