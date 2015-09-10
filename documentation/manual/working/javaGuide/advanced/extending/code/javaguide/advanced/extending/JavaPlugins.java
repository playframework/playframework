package javaguide.advanced.extending;

import akka.actor.ActorRef;
import org.junit.Test;
import play.Application;
import play.Play;
import scala.compat.java8.FutureConverters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static play.test.Helpers.*;
import static akka.pattern.Patterns.ask;

public class JavaPlugins {

    @Test
    public void pluginsShouldBeAccessible() {
        final AtomicReference<MyComponent> myComponentRef = new AtomicReference<MyComponent>();
        Application app = fakeApplication(new HashMap<>(), Arrays.asList(MyPlugin.class.getName()));
        running(app, () -> {
            //#access-plugin
            MyComponent myComponent = Play.application().plugin(MyPlugin.class).getMyComponent();
            //#access-plugin
            assertTrue(myComponent.started);
            myComponentRef.set(myComponent);
        });
        assertTrue(myComponentRef.get().stopped);
    }

    @Test
    public void actorExampleShouldWork() {
        Application app = fakeApplication(new HashMap<>(), Arrays.asList(Actors.class.getName()));
        running(app, () -> {
            ActorRef actor = Actors.getMyActor();
            try {
                assertEquals("hi", FutureConverters.toJava(ask(actor, "hi", 20000)).toCompletableFuture().get(20, TimeUnit.SECONDS));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

}
