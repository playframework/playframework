/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.tests;

// #test-imports
import play.test.*;
import static play.test.Helpers.*;
// #test-imports

import org.junit.Test;
import static org.junit.Assert.*;

import play.Application;

public class FakeApplicationTest {

  public static class Computer {
    public String name = "Macintosh";
    public String introduced = "1984-01-24";

    public static Computer findById(long id) {
      return new Computer();
    }
  }

  String formatted(String s) {
    return s;
  }

  // #test-running-fakeapp
  @Test
  public void findById() {
    running(
        fakeApplication(inMemoryDatabase("test")),
        () -> {
          Computer macintosh = Computer.findById(21l);
          assertEquals("Macintosh", macintosh.name);
          assertEquals("1984-01-24", formatted(macintosh.introduced));
        });
  }
  // #test-running-fakeapp

  private void fakeApps() {

    // #test-fakeapp
    Application fakeApp = fakeApplication();

    Application fakeAppWithMemoryDb = fakeApplication(inMemoryDatabase("test"));
    // #test-fakeapp
  }
}
