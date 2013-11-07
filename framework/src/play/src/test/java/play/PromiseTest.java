/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play;

import org.junit.Test;

import play.libs.F;

import static org.fest.assertions.Assertions.assertThat;

public class PromiseTest {

    @Test
    public void testSupertypeMap() {
        F.Promise<Integer> a = F.Promise.pure(1);

        F.Promise<String> b = a.map(new F.Function<Object, String>() {
          public String apply(Object o) {
            return o.toString();
          }
        });
        assertThat(b.get(1L)).isEqualTo("1");
    }

    @Test
    public void testSupertypeFlatMap() {
        F.Promise<Integer> a = F.Promise.pure(1);

        F.Promise<String> b = a.flatMap(new F.Function<Object, F.Promise<String>>() {
          public F.Promise<String> apply(Object o) {
            return F.Promise.pure(o.toString());
          }
        });
        assertThat(b.get(1L)).isEqualTo("1");
    }
}

