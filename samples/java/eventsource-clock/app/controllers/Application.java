package controllers;

import play.*;
import play.mvc.*;
import play.libs.*;
import play.libs.F.*;

import akka.actor.*;

import java.util.*;
import java.text.*;
import scala.concurrent.duration.Duration;

import static java.util.concurrent.TimeUnit.*;

import views.html.*;

public class Application extends Controller {
    
    final static ActorRef clock = Clock.instance;

    public static Result index() {
        return ok(index.render());
    }
    
    public static Result liveClock() {
        return ok(new EventSource() {  
            public void onConnected() {
               clock.tell(this, null); 
            } 
        });
    }
    
    public static class Clock extends UntypedActor {
        
        static ActorRef instance = Akka.system().actorOf(Props.create(Clock.class));
        
        // Send a TICK message every 100 millis
        static {
            Akka.system().scheduler().schedule(
                Duration.Zero(),
                Duration.create(100, MILLISECONDS),
                instance, "TICK",  Akka.system().dispatcher(),
                null
            );
        }
        
        List<EventSource> sockets = new ArrayList<EventSource>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH mm ss");
        
        public void onReceive(Object message) {

            // Handle connections
            if(message instanceof EventSource) {
                final EventSource eventSource = (EventSource)message;
                
                if(sockets.contains(eventSource)) {                    
                    // Browser is disconnected
                    sockets.remove(eventSource);
                    Logger.info("Browser disconnected (" + sockets.size() + " browsers currently connected)");
                    
                } else {                    
                    // Register disconnected callback 
                    eventSource.onDisconnected(new Callback0() {
                        public void invoke() {
                            getContext().self().tell(eventSource, null);
                        }
                    });                    
                    // New browser connected
                    sockets.add(eventSource);
                    Logger.info("New browser connected (" + sockets.size() + " browsers currently connected)");
                    
                }
                
            }             
            // Tick, send time to all connected browsers
            if("TICK".equals(message)) {
                // Send the current time to all EventSource sockets
                List<EventSource> shallowCopy = new ArrayList<EventSource>(sockets); //prevent ConcurrentModificationException
                for(EventSource es: shallowCopy) {
                    es.sendData(dateFormat.format(new Date()));
                }
                
            }

        }
        
    }
  
}
