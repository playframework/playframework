/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import models.UserId;
import play.mvc.Controller;
import play.mvc.Result;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Application extends Controller {

  @Inject
  public Application() {}

  public Result index() {
    return ok();
  }

  public Result post() {
    return ok();
  }

  public Result withParam(String param) {
    return ok(param);
  }

  public Result user(UserId userId) {
    return ok(userId.getId());
  }

  public Result queryUser(UserId.UserIdQueryParam userId) {
    return ok(userId.getId());
  }

  public Result takeIntEscapes(Integer i) {
    return ok(i.toString());
  }

  public Result takeBool(Boolean b) {
    return ok(b.toString());
  }

  public Result takeBool2(Boolean b) {
    return ok(b.toString());
  }

  public Result takeString(String x) {
    return ok(x);
  }

  public Result takeStringOptional(Optional<String> x) {
    return ok(x.orElse("emptyOptional"));
  }

  public Result takeCharacter(Character x) {
    return ok(x.toString());
  }

  public Result takeCharacterOptional(Optional<Character> x) {
    return ok(x.map(Object::toString).orElse("emptyOptional"));
  }

  public Result takeJavaShort(Short x) {
    return ok(x.toString());
  }

  public Result takeJavaShortOptional(Optional<Short> x) {
    return ok(x.map(Object::toString).orElse("emptyOptional"));
  }

  public Result takeInteger(Integer x) {
    return ok(x.toString());
  }

  public Result takeIntegerOptional(Optional<Integer> x) {
    return ok(x.map(Object::toString).orElse("emptyOptional"));
  }

  public Result takeOptionalInt(OptionalInt x) {
    return ok(x.isPresent() ? String.valueOf(x.getAsInt()) : "emptyOptionalInt");
  }

  public Result takeOptionalLong(OptionalLong x) {
    return ok(x.isPresent() ? String.valueOf(x.getAsLong()) : "emptyOptionalLong");
  }

  public Result takeOptionalDouble(OptionalDouble x) {
    return ok(x.isPresent() ? String.valueOf(x.getAsDouble()) : "emptyOptionalDouble");
  }

  public Result takeJavaListString(List<String> x) {
    return ok(
        x.stream()
            .map(s -> s.isEmpty() ? "emptyStringElement" : s)
            .collect(Collectors.joining(",")));
  }

  public Result takeJavaListStringOptional(Optional<List<String>> x) {
    return ok(
        x.map(
                o ->
                    o.stream()
                        .map(s -> s.isEmpty() ? "emptyStringElement" : s)
                        .collect(Collectors.joining(",")))
            .orElse("emptyOptional"));
  }

  public Result takeJavaListCharacter(List<Character> x) {
    return ok(x.stream().map(Objects::toString).collect(Collectors.joining(",")));
  }

  public Result takeJavaListCharacterOptional(Optional<List<Character>> x) {
    return ok(
        x.map(o -> o.stream().map(Object::toString).collect(Collectors.joining(",")))
            .orElse("emptyOptional"));
  }

  public Result takeJavaListShort(List<Short> x) {
    return ok(x.stream().map(Objects::toString).collect(Collectors.joining(",")));
  }

  public Result takeJavaListShortOptional(Optional<List<Short>> x) {
    return ok(
        x.map(o -> o.stream().map(Object::toString).collect(Collectors.joining(",")))
            .orElse("emptyOptional"));
  }

  public Result takeJavaListInteger(List<Integer> x) {
    return ok(x.stream().map(Objects::toString).collect(Collectors.joining(",")));
  }

  public Result takeJavaListIntegerOptional(Optional<List<Integer>> x) {
    return ok(
        x.map(o -> o.stream().map(Object::toString).collect(Collectors.joining(",")))
            .orElse("emptyOptional"));
  }

  public Result takeStringWithDefault(String x) {
    return ok(x);
  }

  public Result takeStringOptionalWithDefault(Optional<String> x) {
    return ok(x.orElse("emptyOptional"));
  }

  public Result takeCharacterWithDefault(Character c) {
    return ok(c.toString());
  }

  public Result takeCharacterOptionalWithDefault(Optional<Character> x) {
    return ok(x.map(Objects::toString).orElse("emptyOptional"));
  }

  public Result takeJavaShortWithDefault(Short x) {
    return ok(x.toString());
  }

  public Result takeJavaShortOptionalWithDefault(Optional<Short> x) {
    return ok(x.map(Objects::toString).orElse("emptyOptional"));
  }

  public Result takeIntegerWithDefault(Integer x) {
    return ok(x.toString());
  }

  public Result takeIntegerOptionalWithDefault(Optional<Integer> x) {
    return ok(x.map(Objects::toString).orElse("emptyOptional"));
  }

  public Result takeOptionalIntWithDefault(OptionalInt x) {
    return ok(x.isPresent() ? String.valueOf(x.getAsInt()) : "emptyOptionalInt");
  }

  public Result takeOptionalLongWithDefault(OptionalLong x) {
    return ok(x.isPresent() ? String.valueOf(x.getAsLong()) : "emptyOptionalLong");
  }

  public Result takeOptionalDoubleWithDefault(OptionalDouble x) {
    return ok(x.isPresent() ? String.valueOf(x.getAsDouble()) : "emptyOptionalDouble");
  }

  public Result takeJavaListCharacterWithDefault(List<Character> x) {
    return ok(x.stream().map(Objects::toString).collect(Collectors.joining(",")));
  }

  public Result takeJavaListCharacterOptionalWithDefault(Optional<List<Character>> x) {
    return ok(
        x.map(o -> o.stream().map(Object::toString).collect(Collectors.joining(",")))
            .orElse("emptyOptional"));
  }

  public Result takeJavaListStringWithDefault(List<String> x) {
    return ok(
        x.stream()
            .map(s -> s.isEmpty() ? "emptyStringElement" : s)
            .collect(Collectors.joining(",")));
  }

  public Result takeJavaListStringOptionalWithDefault(Optional<List<String>> x) {
    return ok(
        x.map(
                o ->
                    o.stream()
                        .map(s -> s.isEmpty() ? "emptyStringElement" : s)
                        .collect(Collectors.joining(",")))
            .orElse("emptyOptional"));
  }

  public Result takeJavaListShortWithDefault(List<Short> x) {
    return ok(x.stream().map(Objects::toString).collect(Collectors.joining(",")));
  }

  public Result takeJavaListShortOptionalWithDefault(Optional<List<Short>> x) {
    return ok(
        x.map(o -> o.stream().map(Object::toString).collect(Collectors.joining(",")))
            .orElse("emptyOptional"));
  }

  public Result takeJavaListIntegerWithDefault(List<Integer> x) {
    return ok(x.stream().map(Objects::toString).collect(Collectors.joining(",")));
  }

  public Result takeJavaListIntegerOptionalWithDefault(Optional<List<Integer>> x) {
    return ok(
        x.map(o -> o.stream().map(Object::toString).collect(Collectors.joining(",")))
            .orElse("emptyOptional"));
  }

  public Result urlcoding(String dynamic, String _static, String query) {
    return ok(String.format("dynamic=%s static=%s query=%s", dynamic, _static, query));
  }

  public Result keyword(String keyword) {
    return ok(keyword);
  }

  public Result keywordDefault(String keyword) {
    return ok(keyword);
  }

  public Result keywordPath(String keyword) {
    return ok(keyword);
  }

  public Result fixedValue(String parameter) {
    return ok(parameter);
  }

  public Result interpolatorWarning(String parameter) {
    return ok(parameter);
  }
}
