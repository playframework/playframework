package play;

import org.junit.Test;

import play.libs.F;
import play.libs.F.Option;

import static org.fest.assertions.Assertions.assertThat;

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

}

