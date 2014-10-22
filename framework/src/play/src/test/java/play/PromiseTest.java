/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play;

import java.util.concurrent.TimeoutException;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import play.libs.F;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class PromiseTest {

    // timeout
    private Long t = 1000L;

    @Test
    public void testSupertypeMap() {
        F.Promise<Integer> a = F.Promise.pure(1);

        F.Promise<String> b = a.map(new F.Function<Object, String>() {
          public String apply(Object o) {
            return o.toString();
          }
        });
        assertThat(b.get(t)).isEqualTo("1");
    }

    @Test
    public void testSupertypeFlatMap() {
        F.Promise<Integer> a = F.Promise.pure(1);

        F.Promise<String> b = a.flatMap(new F.Function<Object, F.Promise<String>>() {
          public F.Promise<String> apply(Object o) {
            return F.Promise.pure(o.toString());
          }
        });
        assertThat(b.get(t)).isEqualTo("1");
    }

    @Test
    public void testEmptyPromise() {
        F.RedeemablePromise<Integer> a = F.RedeemablePromise.empty();

        F.Promise<String> b = a.map(new F.Function<Integer, String>() {
          public String apply(Integer i) {
            return i.toString();
          }
        });

        a.success(1);

        assertThat(a.get(t)).isEqualTo(1);
        assertThat(b.get(t)).isEqualTo("1");
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testFailPromise() {
        exception.expect(RuntimeException.class);
        F.RedeemablePromise<Integer> a = F.RedeemablePromise.empty();

        a.failure(new RuntimeException("test"));

        a.get(t);
    }

    @Test
    public void testDualSuccessPromise() {
        exception.expect(IllegalStateException.class);
        F.RedeemablePromise<Integer> a = F.RedeemablePromise.empty();

        a.success(1);
        a.success(2);
    }

    @Test
    public void testCompleteWithPromise() {
        F.RedeemablePromise<Integer> a = F.RedeemablePromise.empty();
        F.RedeemablePromise<Integer> b = F.RedeemablePromise.empty();

        a.success(1);

        F.Promise<Void> c = b.completeWith(a);
        assertThat(c.get(t)).isEqualTo(null);
        assertThat(b.get(t)).isEqualTo(1);



        // Complete a second time
        F.Promise<Void> d = b.completeWith(a);

        // And we should get an exception !
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Promise already completed.");
        d.get(t);
    }

    @Test
    public void testTryCompleteWithPromise() {
        F.RedeemablePromise<Integer> a = F.RedeemablePromise.empty();
        F.RedeemablePromise<Integer> b = F.RedeemablePromise.empty();

        a.success(1);

        // Assertions not placed on bottom on this method to enforce
        // the Promise to be completed and avoid raceconditions
        F.Promise<Boolean> c = b.tryCompleteWith(a);
        assertThat(c.get(t)).isEqualTo(true);
        F.Promise<Boolean> d = b.tryCompleteWith(a);
        assertThat(d.get(t)).isEqualTo(false);

        assertThat(b.get(t)).isEqualTo(1);
    }

    @Test
    public void testCombinePromiseSequence() {
        F.Promise<Integer> a = F.Promise.pure(1);
        F.Promise<Integer> b = F.Promise.pure(2);
        F.Promise<Integer> c = F.Promise.pure(3);

        F.Promise<List<Integer>> combined = F.Promise.sequence(Arrays.asList(a, b, c));

        assertThat(combined.get(t)).isEqualTo(Arrays.asList(1, 2, 3));
    }

    @Test
    public void testGetTimeoutExceptions() {
        // Check that Promise.get() translates TimeoutExceptions in failed
        // F.Promise into F.PromiseTimeoutExceptions.
        try {
            F.RedeemablePromise.<Integer>empty().get(1, MILLISECONDS);
            fail("Expected get to throw exception");
        } catch (F.PromiseTimeoutException e){
            // Don't check exception message because the message comes from Scala
        }

        // Check that Promise.get() translates TimeoutExceptions thrown
        // by Await.result() into F.PromiseTimeoutExceptions.
        {
            TimeoutException origT = new TimeoutException("x");
            try {
                F.RedeemablePromise<Integer> p = F.RedeemablePromise.<Integer>empty();
                p.failure(origT);
                p.get(1, MILLISECONDS);
                fail("Expected get to throw exception");
            } catch (F.PromiseTimeoutException e){
                assertThat(e).hasMessage("x");
                assertThat(e.getCause()).isEqualTo(origT);
            }
        }
    }

    @Test
    public void testTimeoutException() {
        // Perform exception check *before* calling get(), because get()
        // translates TimeoutExceptions into F.PromiseTimeoutExceptions,
        // and we want to make sure that timeout() throws
        // F.PromiseTimeoutExceptions directly.
        {
            F.Function<scala.Unit, scala.Unit> failF = new F.Function<scala.Unit, scala.Unit>() {
                public scala.Unit apply(scala.Unit u) throws Throwable {
                    fail("Expected Promise.timeout to throw an exception");
                    return null;
                }
            };
            F.Function<Throwable,scala.Unit> checkThrowableF = new F.Function<Throwable,scala.Unit>() {
                public scala.Unit apply(Throwable t) throws Throwable {
                    if (t instanceof F.PromiseTimeoutException) {
                        assertThat(t).hasMessage("Timeout in promise");
                    } else {
                        fail("Expected Promise.timeout to throw F.PromiseTimeoutException");
                    }
                    return null;
                }
            };
            F.Promise.timeout(10).map(failF).recover(checkThrowableF).get(1, SECONDS);
            F.Promise.timeout(10, MILLISECONDS).map(failF).recover(checkThrowableF).get(1, SECONDS);
        }
    }

}