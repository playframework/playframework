/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

// refs https://github.com/lagom/lagom/issues/3241
@JsonDeserialize(using = ChildDeserializer.class)
public class Child {

  @NonNull Long updatedAt;
  @NonNull String updatedBy;

  @JsonCreator
  public Child(
      @JsonProperty("updatedAt") @NonNull Long updatedAt,
      @JsonProperty("updatedBy") @NonNull String updatedBy) {
    this.updatedAt = updatedAt;
    this.updatedBy = updatedBy;
  }

  public Long getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Long updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Child child = (Child) o;
    return updatedAt.equals(child.updatedAt) && updatedBy.equals(child.updatedBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(updatedAt, updatedBy);
  }

  @Override
  public String toString() {
    return "Child{" + "updatedAt=" + updatedAt + ", updatedBy='" + updatedBy + '\'' + '}';
  }
}
