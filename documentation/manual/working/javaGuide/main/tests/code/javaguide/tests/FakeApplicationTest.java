/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.tests;

import static org.junit.Assert.*;

// #test-imports
import static play.test.Helpers.*;
import play.test.*;
// #test-imports

import org.junit.Test;
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
