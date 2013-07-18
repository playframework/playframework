package test;

import play.mvc.*;
import play.libs.*;
import play.libs.F.*;
import play.api.libs.iteratee.*;
import org.junit.*;

import static org.junit.Assert.*;
import static play.test.Helpers.*;

import java.util.concurrent.*;

class TestController extends Controller {
    private Comet c;

    public TestController(Comet c) {
        this.c = c;
    }

    public Result liveClock() {
        return Results.ok(c);
    }
}

public class JavaCometTest {

    private TestController testController(final Comet c) {
        return new TestController(c);
    }

    @Test
    public void onDisconnectedInvoked() throws Exception {
        final CountDownLatch connected = new CountDownLatch(1);
        final CountDownLatch disconnected = new CountDownLatch(1);
        running(fakeApplication(), new Runnable() {
            public void run() {
                Comet comet = new Comet("parent.clockChanged") {
                    public void onConnected() {
                        connected.countDown();
                        onDisconnected(new Callback0() {
                            public void invoke() {
                                disconnected.countDown();
                            }
                        });
                        close();
                    }
                };
                TestController controller = testController(comet);
                // Consume body
                ((SimpleResult)controller.liveClock()).getWrappedSimpleResult().body().apply(Iteratee$.MODULE$.<byte[]>ignore());
            }
        });
        assertTrue("Connected not invoked", connected.await(5, TimeUnit.SECONDS));
        assertTrue("Disconnected not invoked", disconnected.await(5, TimeUnit.SECONDS));

    }
}
