package javaguide.tests;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;

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
        running(fakeApplication(inMemoryDatabase("test")), new Runnable() {
            public void run() {
                Computer macintosh = Computer.findById(21l);
                assertThat(macintosh.name).isEqualTo("Macintosh");
                assertThat(formatted(macintosh.introduced)).isEqualTo("1984-01-24");
            }
        });
    }
    //#test-running-fakeapp
    
    private void fakeApps() {
      
      //#test-fakeapp
      FakeApplication fakeApp = Helpers.fakeApplication();
      
      FakeApplication fakeAppWithGlobal = fakeApplication(new GlobalSettings() {
        @Override
        public void onStart(Application app) {
          System.out.println("Starting FakeApplication");
        }
      });
      
      FakeApplication fakeAppWithMemoryDb = fakeApplication(inMemoryDatabase("test"));
      //#test-fakeapp
    }

}
