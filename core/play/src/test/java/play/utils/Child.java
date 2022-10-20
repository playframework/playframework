/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

// refs https://github.com/lagom/lagom/issues/3241
@JsonDeserialize(using = ChildDeserializer.class)
public class Child {

  private final @NonNull Long updatedAt;
  private final @NonNull String updatedBy;

  @JsonCreator
  public Child(@NonNull Long updatedAt, @NonNull String updatedBy) {
    this.updatedAt = updatedAt;
    this.updatedBy = updatedBy;
  }

  public Long getUpdatedAt() {
    return updatedAt;
  }

  public String getUpdatedBy() {
    return updatedBy;
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
