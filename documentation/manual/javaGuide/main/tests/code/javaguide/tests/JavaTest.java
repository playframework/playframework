package javaguide.tests;

import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.libs.F.*;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;

public class JavaTest {

    static class Computer {
        String name = "Macintosh";
        String introduced = "1984-01-24";

        static Computer findById(long id) {
            return new Computer();
        }
    }

    String formatted(String s) {
        return s;
    }

    //#fakeapp
    @Test
    public void findById() {
        running(fakeApplication(), new Runnable() {
            public void run() {
                Computer macintosh = Computer.findById(21l);
                assertThat(macintosh.name).isEqualTo("Macintosh");
                assertThat(formatted(macintosh.introduced)).isEqualTo("1984-01-24");
            }
        });
    }
    //#fakeapp

}
