/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play;

import org.junit.Test;

import play.libs.F;
import play.libs.F.Option;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

// see http://docs.oracle.com/javase/7/docs/api/java/util/Collection.html#toArray(T[])
public class OptionTest {

    @Test
    public void testSomeToArray() {
        F.Option<String> a = Option.Some("a");
        assertThat(a.toArray()).isEqualTo(new Object[]{"a"});
        assertThat(a.toArray(new String[]{})).isEqualTo(new String[]{"a"});
        assertThat(a.toArray(new CharSequence[]{})).isEqualTo(new String[]{"a"});
        String[] b = {null, "b", null , "c"};
        assertThat(a.toArray(b)).isEqualTo(new String[]{"a", null, null, null});
    }

    @Test
    public void testNoneToArray() {
        F.Option<String> a = Option.<String>None();
        assertThat(a.toArray()).isEqualTo(new Object[]{});
        assertThat(a.toArray(new String[]{})).isEqualTo(new String[]{});
        assertThat(a.toArray(new CharSequence[]{})).isEqualTo(new String[]{});
        String[] b = {null, "b", null , "c"};
        assertThat(a.toArray(b)).isEqualTo(new String[]{null, null, null, null});
    }

    @Test(expected=ArrayStoreException.class)
    public void throwArrayStoreException() {
        F.Option<String> a = Option.Some("a");
        a.toArray(new Integer[]{});
    }

    @Test
    public void testEquals() {
        assertTrue(F.Option.<Integer>Some(12345).equals(F.Option.<Integer>Some(12345)));
        assertTrue(F.Option.<String>Some("").equals(F.Option.<String>Some("")));
        assertTrue(F.Option.<String>Some("foo").equals(F.Option.<String>Some("foo")));
        assertTrue(F.Option.None().equals(F.Option.None()));
        assertTrue(F.Option.Some(null).equals(F.Option.Some(null)));

        assertFalse(F.Option.<Integer>Some(12345).equals(F.Option.<Integer>Some(98765)));
        assertFalse(F.Option.<String>Some("").equals(F.Option.<String>Some("foo")));
        assertFalse(F.Option.<Integer>Some(12345).equals(F.Option.<Integer>None()));
        assertFalse(F.Option.<Integer>Some(null).equals(F.Option.<Integer>None()));
    }

    @Test
    public void testHashCode() {
        assertThat(F.Option.<Integer>Some(12345).hashCode()).isEqualTo(F.Option.<Integer>Some(12345).hashCode());
        assertThat(F.Option.<String>Some("").hashCode()).isEqualTo(F.Option.<String>Some("").hashCode());
        assertThat(F.Option.<String>Some("foo").hashCode()).isEqualTo(F.Option.<String>Some("foo").hashCode());
        assertThat(F.Option.None().hashCode()).isEqualTo(F.Option.None().hashCode());
        assertThat(F.Option.Some(null).hashCode()).isEqualTo(F.Option.Some(null).hashCode());

        assertThat(F.Option.<Integer>Some(12345).hashCode()).isNotEqualTo(F.Option.<Integer>Some(98765).hashCode());
        assertThat(F.Option.<String>Some("").hashCode()).isNotEqualTo(F.Option.<String>Some("foo").hashCode());
        assertThat(F.Option.<Integer>Some(12345).hashCode()).isNotEqualTo(F.Option.None().hashCode());
        assertThat(F.Option.Some(null).hashCode()).isNotEqualTo(F.Option.None().hashCode());
    }

}

