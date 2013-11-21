/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import play.libs.F;

import static org.fest.assertions.Assertions.assertThat;

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
}

