package javaguide.tests;

import static play.test.Helpers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import play.Application;
import play.GlobalSettings;
import play.test.FakeApplication;
import play.test.Helpers;

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

    private void fakeApps() {

      //#test-fakeapp
      Application fakeApp = Helpers.fakeApplication();

      Application fakeAppWithGlobal = fakeApplication(new GlobalSettings() {
        @Override
        public void onStart(Application app) {
          System.out.println("Starting FakeApplication");
        }
      });

      Application fakeAppWithMemoryDb = fakeApplication(inMemoryDatabase("test"));
      //#test-fakeapp
    }

}
