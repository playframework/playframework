/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

// refs https://github.com/lagom/lagom/issues/3241
public class Parent {

  private final Long createdAt;
  private final Child child;
  private final Long updatedAt;
  private final String updatedBy;

  @JsonCreator
  public Parent(
      @JsonProperty("createdAt") Long createdAt,
      @JsonProperty("child") Child child,
      @JsonProperty("updatedAt") Long updatedAt,
      @JsonProperty("updatedBy") String updatedBy) {
    this.createdAt = createdAt;
    this.child = child;
    this.updatedAt = updatedAt;
    this.updatedBy = updatedBy;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public Child getChild() {
    return child;
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
    Parent parent = (Parent) o;
    return createdAt.equals(parent.createdAt)
        && child.equals(parent.child)
        && updatedAt.equals(parent.updatedAt)
        && updatedBy.equals(parent.updatedBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdAt, child, updatedAt, updatedBy);
  }

  @Override
  public String toString() {
    return "Parent{"
        + "createdAt="
        + createdAt
        + ", child="
        + child
        + ", updatedAt="
        + updatedAt
        + ", updatedBy='"
        + updatedBy
        + '\''
        + '}';
  }
}
