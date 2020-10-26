/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/** */
public class PrimitiveNumericJavaPojo {

  private final int intValue;
  private final long longValue;
  private final float floatValue;
  private final double doubleValue;

  @JsonCreator
  public PrimitiveNumericJavaPojo(
      @JsonProperty("intValue") int intValue,
      @JsonProperty("longValue") long longValue,
      @JsonProperty("floatValue") float floatValue,
      @JsonProperty("doubleValue") double doubleValue) {
    this.intValue = intValue;
    this.longValue = longValue;
    this.floatValue = floatValue;
    this.doubleValue = doubleValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PrimitiveNumericJavaPojo that = (PrimitiveNumericJavaPojo) o;
    return intValue == that.intValue
        && longValue == that.longValue
        && Float.compare(that.floatValue, floatValue) == 0
        && Double.compare(that.doubleValue, doubleValue) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(intValue, longValue, floatValue, doubleValue);
  }

  @Override
  public String toString() {
    return "PrimitiveNumericJavaPojo{"
        + "intValue="
        + intValue
        + ", longValue="
        + longValue
        + ", floatValue="
        + floatValue
        + ", doubleValue="
        + doubleValue
        + '}';
  }
}
