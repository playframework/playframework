package play;

import org.junit.Test;

import play.libs.F.Promise;
import play.libs.F.Tuple;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

public class PromiseTest {

    @Test
    public void zip() {
        Promise<String> a = Promise.pure("a");
        Promise<Integer> b = Promise.pure(10);
        Tuple<String, Integer> ab = a.zip(b).get();
        assertThat(ab._1).isEqualTo("a");
        assertThat(ab._2).isEqualTo(10);
    }

}

