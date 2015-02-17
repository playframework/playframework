/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play;

import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import play.api.libs.iteratee.ExecutionTest;
import play.libs.F;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class PromiseTest extends ExecutionTest {

    // timeout
    private Long t = 1000L;

    @Rule
    public ExpectedException exception = ExpectedException.none();

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
            F.RedeemablePromise.empty().get(1, MILLISECONDS);
            fail("Expected get to throw exception");
        } catch (F.PromiseTimeoutException e){
            // Don't check exception message because the message comes from Scala
        }

        // Check that Promise.get() translates TimeoutExceptions thrown
        // by Await.result() into F.PromiseTimeoutExceptions.
        {
            TimeoutException origT = new TimeoutException("x");
            try {
                F.RedeemablePromise<Integer> p = F.RedeemablePromise.empty();
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
    public void testWrapScalaFuture() {
        scala.concurrent.Future<Integer> f = akka.dispatch.Futures.successful(1);
        F.Promise<Integer> p = F.Promise.wrap(f);
        assertThat(p.wrapped()).isEqualTo(f);
    }

    @Test
    public void testYieldValue() {
        F.Promise<Integer> p = F.Promise.pure(1);
        assertThat(p.get(5000)).isEqualTo(1);
        assertThat(p.get(5, SECONDS)).isEqualTo(1);
    }

    @Test
    public void testThrowing() {
        F.Promise<String> p = F.Promise.throwing(new RuntimeException("x"));
        try {
            p.get(5, SECONDS);
            fail("Expected F.Promise.throwing promise to throw exception on get");
        } catch (RuntimeException e){
            assertThat(e).hasMessage("x");
        }
    }

    @Test
    public void testCreateFromFunction() {
        F.Promise<Integer> p = F.Promise.promise(() -> 1);
        assertThat(p.get(t)).isEqualTo(1);
    }

    @Test
    public void testCreateFromFunctionWithEC() {
        mustExecute(1, ec -> {
            F.Promise<Integer> p = F.Promise.promise(() -> 1, ec);
            assertThat(p.get(t)).isEqualTo(1);
        });
    }

    @Test
    public void testDelayed() {
        F.Promise<Integer> p = F.Promise.delayed(() -> 1, 1, MILLISECONDS);
        assertThat(p.get(t)).isEqualTo(1);
    }

    @Test
    public void testDelayedWithEC() {
        mustExecute(1, ec -> {
            F.Promise<Integer> p = F.Promise.delayed(() -> 1, 1, MILLISECONDS, ec);
            assertThat(p.get(t)).isEqualTo(1);
        });
    }

    @Test
    public void testRedeemValue() throws Exception {
        F.RedeemablePromise<Integer> p = F.RedeemablePromise.empty();
        LinkedBlockingQueue<Integer> invocations = new LinkedBlockingQueue<>();
        p.onRedeem(invocations::offer);
        p.success(99);
        assertThat(invocations.poll(5, SECONDS)).isEqualTo(99);
    }

    @Test
    public void testRedeemValueWithEC() throws Exception {
        mustExecute(1, ec -> {
            F.RedeemablePromise<Integer> p = F.RedeemablePromise.empty();
            LinkedBlockingQueue<Integer> invocations = new LinkedBlockingQueue<>();
            p.onRedeem(invocations::offer, ec);
            p.success(99);
            assertThat(invocations.poll(5, SECONDS)).isEqualTo(99);
        });
    }

    @Test
    public void testMap() {
        F.Promise<Integer> p = F.Promise.pure(1);
        F.Promise<Integer> mapped = p.map(x -> 2 * x);
        assertThat(mapped.get(t)).isEqualTo(2);
    }

    @Test
    public void testMapWithEC() {
        mustExecute(1, ec -> {
            F.Promise<Integer> p = F.Promise.pure(1);
            F.Promise<Integer> mapped = p.map(x -> 2 * x, ec);
            assertThat(mapped.get(t)).isEqualTo(2);
        });
    }

    @Test
    public void testSupertypeMap() {
        F.Promise<Integer> a = F.Promise.pure(1);
        F.Function<Object, String> f = Object::toString;
        F.Promise<String> b = a.map(f);
        assertThat(b.get(t)).isEqualTo("1");
    }

    @Test
    public void testFlatMap() {
        F.Promise<Integer> p = F.Promise.pure(1);
        F.Promise<Integer> mapped = p.flatMap(x -> F.Promise.pure(2 * x));
        assertThat(mapped.get(t)).isEqualTo(2);
    }

    @Test
    public void testFlatMapWithEC() {
        mustExecute(1, ec -> {
            F.Promise<Integer> p = F.Promise.pure(1);
            F.Promise<Integer> mapped = p.flatMap(x -> F.Promise.pure(2 * x), ec);
            assertThat(mapped.get(t)).isEqualTo(2);
        });
    }

    @Test
    public void testSupertypeFlatMap() {
        F.Promise<Integer> a = F.Promise.pure(1);
        F.Function<Object, F.Promise<String>> f = o -> F.Promise.pure(o.toString());
        F.Promise<String> b = a.flatMap(f);
        assertThat(b.get(t)).isEqualTo("1");
    }

    @Test
    public void testFilter() {
        F.Promise<Integer> p = F.Promise.pure(1);
        F.Promise<Integer> filtered = p.filter(x -> x > 0);
        assertThat(filtered.get(t)).isEqualTo(1);
    }

    @Test
    public void testFilterWithEC() {
        mustExecute(1, ec -> {
            F.Promise<Integer> p = F.Promise.pure(1);
            F.Promise<Integer> filtered = p.filter(x -> x > 0, ec);
            assertThat(filtered.get(t)).isEqualTo(1);
        });
    }

    @Test
    public void testFilterToFailure() {
        F.Promise<Integer> p = F.Promise.pure(-1);
        F.Promise<Integer> filtered = p.filter(x -> x > 0);
        try {
            filtered.get(t);
            fail("Expected filtered promise to throw NoSuchElementException on get");
        } catch (NoSuchElementException e){
            assertThat(e).hasMessage("Future.filter predicate is not satisfied");
        }
    }

    @Test
    public void testFilterToFailureWithEC() {
        mustExecute(1, ec -> {
            F.Promise<Integer> p = F.Promise.pure(-1);
            F.Promise<Integer> filtered = p.filter(x -> x > 0, ec);
            try {
                filtered.get(t);
                fail("Expected filtered promise to throw NoSuchElementException on get");
            } catch (NoSuchElementException e){
                assertThat(e).hasMessage("Future.filter predicate is not satisfied");
            }
        });
    }

    @Test
    public void testTransformOnSuccess() {
        F.Promise<Integer> p = F.Promise.pure(1);
        F.Promise<Integer> mapped = p.transform(x -> 2 * x, t -> t);
        assertThat(mapped.get(5, SECONDS)).isEqualTo(2);
    }

    @Test
    public void testTransformOnSuccessWithEC() {
        mustExecute(1, ec -> {
            F.Promise<Integer> p = F.Promise.pure(1);
            F.Promise<Integer> mapped = p.transform(x -> 2 * x, t -> t, ec);
            assertThat(mapped.get(5, SECONDS)).isEqualTo(2);
        });
    }

    @Test
    public void testTransformOnFailure() {
        F.Promise<String> p = F.Promise.throwing(new RuntimeException("1"));
        try {
            p.transform(x -> x, t -> new RuntimeException("2")).get(5, SECONDS);
            fail("Expected transformed promise to throw exception on get");
        } catch (RuntimeException e){
            assertThat(e).hasMessage("2");
        }
    }

    @Test
    public void testTransformOnFailureWithEC() {
        mustExecute(1, ec -> {
            F.Promise<String> p = F.Promise.throwing(new RuntimeException("1"));
            try {
                p.transform(x -> x, t -> new RuntimeException("2"), ec).get(5, SECONDS);
                fail("Expected transformed promise to throw exception on get");
            } catch (RuntimeException e){
                assertThat(e).hasMessage("2");
            }
        });
    }

    @Test
    public void testEmpty() {
        F.RedeemablePromise<Integer> a = F.RedeemablePromise.empty();
        F.Promise<String> b = a.map(Object::toString);
        a.success(1);
        assertThat(a.get(t)).isEqualTo(1);
        assertThat(b.get(t)).isEqualTo("1");
    }

    @Test
    public void testFail() {
        exception.expect(RuntimeException.class);
        F.RedeemablePromise<Integer> a = F.RedeemablePromise.empty();
        a.failure(new RuntimeException("test"));
        a.get(t);
    }

    @Test
    public void testRecover() {
        F.Promise<Integer> p = F.Promise.throwing(new RuntimeException("x"));
        F.Promise<Integer> recovered = p.recover(x -> 99);
        assertThat(recovered.get(t)).isEqualTo(99);
    }

    @Test
    public void testRecoverWithEC() {
        mustExecute(1, ec -> {
            F.Promise<Integer> p = F.Promise.throwing(new RuntimeException("x"));
            F.Promise<Integer> recovered = p.recover(x -> 99, ec);
            assertThat(recovered.get(t)).isEqualTo(99);
        });
    }

    @Test
    public void testRecoverWith() {
        F.Promise<Integer> p = F.Promise.throwing(new RuntimeException("x"));
        F.Promise<Integer> recovered = p.recoverWith(x -> F.Promise.pure(99));
        assertThat(recovered.get(t)).isEqualTo(99);
    }

    @Test
    public void testRecoverWithWithEC() {
        mustExecute(1, ec -> {
            F.Promise<Integer> p = F.Promise.throwing(new RuntimeException("x"));
            F.Promise<Integer> recovered = p.recoverWith(x -> F.Promise.pure(99), ec);
            assertThat(recovered.get(t)).isEqualTo(99);
        });
    }

    @Test
    public void testFallbackTo() {
        F.Promise<Integer> p1 = F.Promise.throwing(new RuntimeException("x"));
        F.Promise<Integer> p2 = p1.fallbackTo(F.Promise.pure(42));
        assertThat(p2.get(t)).isEqualTo(42);
    }

    @Test
    public void testUnusedFallback() {
        F.Promise<Integer> p1 = F.Promise.pure(1);
        F.Promise<Integer> p2 = p1.fallbackTo(F.Promise.pure(2));
        assertThat(p2.get(t)).isEqualTo(1);
    }

    @Test
    public void testFallbackFailed() {
        F.Promise<Integer> p1 = F.Promise.throwing(new RuntimeException("1"));
        F.Promise<Integer> p2 = p1.fallbackTo(F.Promise.throwing(new RuntimeException("2")));
        try {
            p2.get(5, SECONDS);
            fail("Expected promise to throw exception on get");
        } catch (RuntimeException e){
            assertThat(e).hasMessage("1");
        }
    }

    @Test
    public void testDualSuccess() {
        exception.expect(IllegalStateException.class);
        F.RedeemablePromise<Integer> a = F.RedeemablePromise.empty();
        a.success(1);
        a.success(2);
    }

    @Test
    public void testCompleteWith() {
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
    public void testTryCompleteWith() {
        F.RedeemablePromise<Integer> a = F.RedeemablePromise.empty();
        F.RedeemablePromise<Integer> b = F.RedeemablePromise.empty();

        a.success(1);

        // Assertions not placed on bottom on this method to enforce
        // the Promise to be completed and avoid race conditions
        F.Promise<Boolean> c = b.tryCompleteWith(a);
        assertThat(c.get(t)).isEqualTo(true);
        F.Promise<Boolean> d = b.tryCompleteWith(a);
        assertThat(d.get(t)).isEqualTo(false);

        assertThat(b.get(t)).isEqualTo(1);
    }

    @Test
    public void testTimeout() {
        assertThat(F.Promise.timeout("x", 10).get(1, SECONDS)).isEqualTo("x");
        assertThat(F.Promise.timeout("x", 10, MILLISECONDS).get(1, SECONDS)).isEqualTo("x");
    }

    @Test
    public void testTimeoutException() {
        try {
            F.Promise.timeout(10).get(1, SECONDS);
            fail("Expected Promise.timeout to throw TimeoutException on get");
        } catch (F.PromiseTimeoutException e){
            assertThat(e).hasMessage("Timeout in promise");
        }

        try {
            F.Promise.timeout(10, MILLISECONDS).get(1, SECONDS);
            fail("Expected Promise.timeout to throw TimeoutException on get");
        } catch (F.PromiseTimeoutException e){
            assertThat(e).hasMessage("Timeout in promise");
        }
    }

    @Test
    public void testZip() {
        F.Promise<Integer> a = F.Promise.pure(1);
        F.Promise<String> b = F.Promise.pure("2");
        F.Tuple<Integer, String> zipped = a.zip(b).get(t);
        assertThat(zipped._1).isEqualTo(1);
        assertThat(zipped._2).isEqualTo("2");
    }

    @Test
    public void testOrLeft() {
        F.RedeemablePromise<Integer> left = F.RedeemablePromise.empty();
        F.RedeemablePromise<String> right = F.RedeemablePromise.empty();
        F.Promise<F.Either<Integer, String>> either = left.or(right);
        left.success(1);
        F.Either<Integer, String> result = either.get(t);
        assertThat(result.left.get()).isEqualTo(1);
        assertThat(result.right.isDefined()).isFalse();
    }

    @Test
    public void testOrRight() {
        F.RedeemablePromise<Integer> left = F.RedeemablePromise.empty();
        F.RedeemablePromise<String> right = F.RedeemablePromise.empty();
        F.Promise<F.Either<Integer, String>> either = left.or(right);
        right.success("2");
        F.Either<Integer, String> result = either.get(t);
        assertThat(result.left.isDefined()).isFalse();
        assertThat(result.right.get()).isEqualTo("2");
    }

    @Test
    @SuppressWarnings({"unchecked"})
    public void testSequenceWithVarargs() {
        F.Promise<Integer> a = F.Promise.pure(1);
        F.Promise<Integer> b = F.Promise.pure(2);
        F.Promise<Integer> c = F.Promise.pure(3);

        F.Promise<List<Integer>> combined = F.Promise.sequence(a, b, c);

        assertThat(combined.get(t)).isEqualTo(Arrays.asList(1, 2, 3));
    }

    @Test
    public void testSequenceWithIterable() {
        F.Promise<Integer> a = F.Promise.pure(1);
        F.Promise<Integer> b = F.Promise.pure(2);
        F.Promise<Integer> c = F.Promise.pure(3);

        F.Promise<List<Integer>> combined = F.Promise.sequence(Arrays.asList(a, b, c));

        assertThat(combined.get(t)).isEqualTo(Arrays.asList(1, 2, 3));
    }

    @Test
    public void testSequenceWithStream() {
        Stream<F.Promise<Integer>> promises = Arrays.asList(1, 2, 3).stream().map(F.Promise::pure);
        F.Promise<List<Integer>> combined = F.Promise.sequence(promises::iterator);
        assertThat(combined.get(t)).isEqualTo(Arrays.asList(1, 2, 3));
    }
}
