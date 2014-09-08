package java8guide.tests;

import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.libs.F.*;
import static play.test.Helpers.*;
import static org.junit.Assert.*;

public class FakeApplicationTest {

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

    //#test-running-fakeapp
    @Test
    public void findById() {
        running(fakeApplication(inMemoryDatabase("test")), () -> {
            Computer macintosh = Computer.findById(21l);
            assertEquals("Macintosh", macintosh.name);
            assertEquals("1984-01-24", formatted(macintosh.introduced));
        });
    }
    //#test-running-fakeapp

}
