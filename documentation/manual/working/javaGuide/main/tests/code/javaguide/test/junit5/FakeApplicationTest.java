/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.test.junit5;

import static org.junit.jupiter.api.Assertions.assertEquals;

// #test-imports
import static play.test.Helpers.*;
// #test-imports

import org.junit.jupiter.api.Test;
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
  void findById() {
    running(
        fakeApplication(inMemoryDatabase("test")),
        () -> {
          Computer macintosh = Computer.findById(21L);
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
