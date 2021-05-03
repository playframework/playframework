/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

// refs https://github.com/lagom/lagom/issues/3241
public class Parent {

  @NonNull Long createdAt;
  Child child;
  @NonNull Long updatedAt;
  @NonNull String updatedBy;

  @JsonCreator
  public Parent(
      @JsonProperty("createdAt") @NonNull Long createdAt,
      @JsonProperty("child") Child child,
      @JsonProperty("udpatedAt") @NonNull Long updatedAt,
      @JsonProperty("udpatedBy") @NonNull String updatedBy) {
    this.createdAt = createdAt;
    this.child = child;
    this.updatedAt = updatedAt;
    this.updatedBy = updatedBy;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
  }

  public Child getChild() {
    return child;
  }

  public void setChild(Child child) {
    this.child = child;
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
