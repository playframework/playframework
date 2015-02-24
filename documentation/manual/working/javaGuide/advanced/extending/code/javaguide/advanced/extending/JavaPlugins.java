package javaguide.advanced.extending;

import akka.actor.ActorRef;
import org.junit.Test;
import play.Application;
import play.Play;
import play.libs.F;
import play.test.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static play.test.Helpers.*;
import static akka.pattern.Patterns.ask;

public class JavaPlugins {

    @Test
    public void pluginsShouldBeAccessible() {
        final AtomicReference<MyComponent> myComponentRef = new AtomicReference<MyComponent>();
        Application app = fakeApplication(new HashMap<String, Object>(), Arrays.asList(MyPlugin.class.getName()));
        running(app, new Runnable() {
            public void run() {
                //#access-plugin
                MyComponent myComponent = Play.application().plugin(MyPlugin.class).getMyComponent();
                //#access-plugin
                assertTrue(myComponent.started);
                myComponentRef.set(myComponent);
            }
        });
        assertTrue(myComponentRef.get().stopped);
    }

    @Test
    public void actorExampleShouldWork() {
        Application app = fakeApplication(new HashMap<String, Object>(), Arrays.asList(Actors.class.getName()));
        running(app, new Runnable() {
            public void run() {
                ActorRef actor = Actors.getMyActor();
                assertEquals("hi", F.Promise.wrap(ask(actor, "hi", 20000)).get(20000));
            }
        });
    }

}
