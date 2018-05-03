/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs;

import java.time.Instant;
import java.util.Optional;

public class JavaPOJO {

  private String foo;
  private String bar;
  private Instant instant;
  private Optional<Integer> optNumber;

  public String getFoo() {
    return foo;
  }

  public void setFoo(String foo) {
    this.foo = foo;
  }

  public String getBar() {
    return bar;
  }

  public void setBar(String bar) {
    this.bar = bar;
  }

  public Instant getInstant() {
    return instant;
  }

  public void setInstant(Instant instant) {
    this.instant = instant;
  }

  public Optional<Integer> getOptNumber() {
    return optNumber;
  }

  public void setOptNumber(Optional<Integer> optNumber) {
    this.optNumber = optNumber;
  }
}
