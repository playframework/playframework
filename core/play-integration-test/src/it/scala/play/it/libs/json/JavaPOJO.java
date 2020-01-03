/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.libs.json;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

public class JavaPOJO {

  private String foo;
  private String bar;
  private Instant instant;
  private Optional<Integer> optNumber;
  private OptionalInt optionalInt;

  public JavaPOJO() {
    // empty constructor useful for Jackson
  }

  public JavaPOJO(String foo, String bar, Instant instant, Optional<Integer> optNumber, OptionalInt optionalInt) {
    this.foo = foo;
    this.bar = bar;
    this.instant = instant;
    this.optNumber = optNumber;
    this.optionalInt = optionalInt;
  }

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

  public OptionalInt getOptionalInt() {
    return optionalInt;
  }

  public void setOptionalInt(OptionalInt optionalInt) {
    this.optionalInt = optionalInt;
  }
}
