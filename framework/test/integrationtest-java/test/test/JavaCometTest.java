package test;

import play.mvc.*;
import play.*;
import play.libs.*;
import play.libs.F.*;
import play.api.mvc.ChunkedResult;
import play.api.libs.iteratee.*;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
import static play.test.Helpers.*;
import java.util.concurrent.atomic.*; 
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
  Integer callbackCalled = 0;

  private TestController testController(final Comet c) {
    return new TestController(c);
  }

  @Test
  public void onDisconnectedInvoked() {
    final AtomicInteger callbackCalled = new AtomicInteger(0);
    final CountDownLatch latch = new CountDownLatch(1);
    running(fakeApplication(), new Runnable() {
      public void run() {
        Comet comet = new Comet("parent.clockChanged") {  
          public void onConnected() {}
        };
        TestController controller = testController(comet);
        ChunkedResult<Object> result = (ChunkedResult<Object>)controller.liveClock().getWrappedResult();
        result.chunks().apply(Iteratee$.MODULE$.ignore());
        comet.onDisconnected(new Callback0() {
          public void invoke() {
            callbackCalled.set(1);
            latch.countDown();
          }
        });
        comet.close();
        try {
          latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch(Throwable t) {}
        assertEquals(1, callbackCalled.get());
      }
    });
  } 
}
