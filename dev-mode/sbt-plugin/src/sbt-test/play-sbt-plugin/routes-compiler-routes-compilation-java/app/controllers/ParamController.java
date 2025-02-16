/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Optional;
import play.mvc.Controller;
import play.mvc.Result;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class ParamController<T> extends Controller {
  public Result path(T x) {
    return ok(x.toString());
  }

  public Result query(T x) {
    return ok(x.toString());
  }

  public Result queryDefault(T x) {
    return ok(x.toString());
  }

  public Result queryFixed(T x) {
    return ok(x.toString());
  }

  public Result queryNullable(T x) {
    return ok(x == null ? "null" : x.toString());
  }

  public Result queryOptional(Optional<T> x) {
    return ok(x.map(Object::toString).orElse("emptyOptional"));
  }

  public Result queryOptionalDefault(Optional<T> x) {
    return ok(x.map(Object::toString).orElse("emptyOptional"));
  }

  public Result queryList(List<T> x) {
    return ok(x.stream().map(this::listElementToString).collect(joining(",")));
  }

  public Result queryListDefault(List<T> x) {
    return ok(x.stream().map(this::listElementToString).collect(joining(",")));
  }

  public Result queryListNullable(List<T> x) {
    return ok(x == null ? "null" : x.stream().map(this::listElementToString).collect(joining(",")));
  }

  public Result queryListOptional(Optional<List<T>> x) {
    return ok(
        x.map(o -> o.stream().map(this::listElementToString).collect(joining(","))).orElse("emptyOptional"));
  }

  public Result queryListOptionalDefault(Optional<List<T>> x) {
    return ok(
        x.map(o -> o.stream().map(this::listElementToString).collect(joining(","))).orElse("emptyOptional"));
  }

  protected String listElementToString(T s) {
    return s.toString();
  }
}
