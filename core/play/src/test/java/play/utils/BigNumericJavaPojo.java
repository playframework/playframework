/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/** */
public class BigNumericJavaPojo {

  private final BigInteger intValue;

  private final BigDecimal floatValue;

  @JsonCreator
  public BigNumericJavaPojo(
      @JsonProperty("intValue") BigInteger intValue,
      @JsonProperty("floatValue") BigDecimal floatValue) {
    this.intValue = intValue;
    this.floatValue = floatValue;
  }

  public BigInteger getIntValue() {
    return intValue;
  }

  public BigDecimal getFloatValue() {
    return floatValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BigNumericJavaPojo that = (BigNumericJavaPojo) o;
    return Objects.equals(intValue, that.intValue) && Objects.equals(floatValue, that.floatValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(intValue, floatValue);
  }

  @Override
  public String toString() {
    return "BigNumericJavaPojo{" + "intValue=" + intValue + ", floatValue=" + floatValue + '}';
  }
}
