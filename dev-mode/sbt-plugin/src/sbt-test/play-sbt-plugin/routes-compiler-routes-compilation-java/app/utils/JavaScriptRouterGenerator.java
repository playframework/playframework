/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package utils;

import static controllers.routes.javascript.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import play.api.routing.JavaScriptReverseRoute;
import play.api.routing.JavaScriptReverseRouter;
import play.libs.Scala;
import scala.Option;

public class JavaScriptRouterGenerator {

  public static void main(String[] args) throws IOException {
    var routes = new ArrayList<JavaScriptReverseRoute>();
    routes.addAll(applicationControllerRoutes());
    routes.addAll(methodControllerRoutes());
    routes.addAll(assetsControllerRoutes());
    routes.addAll(booleanControllerRoutes());
    routes.addAll(characterControllerRoutes());
    routes.addAll(stringControllerRoutes());
    routes.addAll(shortControllerRoutes());
    routes.addAll(integerControllerRoutes());
    routes.addAll(longControllerRoutes());
    routes.addAll(optionalControllerRoutes());
    routes.addAll(doubleControllerRoutes());
    routes.addAll(floatControllerRoutes());
    routes.addAll(uuidControllerRoutes());
    routes.addAll(userControllerRoutes());
    var jsFile =
        JavaScriptReverseRouter.apply("jsRoutes", Option.empty(), "localhost", Scala.toSeq(routes))
            .body();

    // Add module exports for node
    var jsModule = jsFile + "\nmodule.exports = jsRoutes";

    var path = Paths.get(args[0]);
    Files.createDirectories(path.getParent());
    Files.writeString(path, jsModule);
  }

  private static List<JavaScriptReverseRoute> booleanControllerRoutes() {
    return List.of(
        BooleanController.path(),
        BooleanController.query(),
        BooleanController.queryDefault(),
        BooleanController.queryFixed(),
        BooleanController.queryNullable(),
        BooleanController.queryOptional(),
        BooleanController.queryOptionalDefault(),
        BooleanController.queryList(),
        BooleanController.queryListDefault(),
        BooleanController.queryListNullable(),
        BooleanController.queryListOptional(),
        BooleanController.queryListOptionalDefault());
  }

  private static List<JavaScriptReverseRoute> characterControllerRoutes() {
    return List.of(
        CharacterController.path(),
        CharacterController.query(),
        CharacterController.queryDefault(),
        CharacterController.queryFixed(),
        CharacterController.queryNullable(),
        CharacterController.queryOptional(),
        CharacterController.queryOptionalDefault(),
        CharacterController.queryList(),
        CharacterController.queryListDefault(),
        CharacterController.queryListNullable(),
        CharacterController.queryListOptional(),
        CharacterController.queryListOptionalDefault());
  }

  private static List<JavaScriptReverseRoute> stringControllerRoutes() {
    return List.of(
        StringController.path(),
        StringController.query(),
        StringController.queryDefault(),
        StringController.queryFixed(),
        StringController.queryNullable(),
        StringController.queryOptional(),
        StringController.queryOptionalDefault(),
        StringController.queryList(),
        StringController.queryListDefault(),
        StringController.queryListNullable(),
        StringController.queryListOptional(),
        StringController.queryListOptionalDefault());
  }

  private static List<JavaScriptReverseRoute> shortControllerRoutes() {
    return List.of(
        ShortController.path(),
        ShortController.query(),
        ShortController.queryDefault(),
        ShortController.queryFixed(),
        ShortController.queryNullable(),
        ShortController.queryOptional(),
        ShortController.queryOptionalDefault(),
        ShortController.queryList(),
        ShortController.queryListDefault(),
        ShortController.queryListNullable(),
        ShortController.queryListOptional(),
        ShortController.queryListOptionalDefault());
  }

  private static List<JavaScriptReverseRoute> integerControllerRoutes() {
    return List.of(
        IntegerController.path(),
        IntegerController.query(),
        IntegerController.queryDefault(),
        IntegerController.queryFixed(),
        IntegerController.queryNullable(),
        IntegerController.queryOptional(),
        IntegerController.queryOptionalDefault(),
        IntegerController.queryList(),
        IntegerController.queryListDefault(),
        IntegerController.queryListNullable(),
        IntegerController.queryListOptional(),
        IntegerController.queryListOptionalDefault());
  }

  private static List<JavaScriptReverseRoute> longControllerRoutes() {
    return List.of(
        LongController.path(),
        LongController.query(),
        LongController.queryDefault(),
        LongController.queryFixed(),
        LongController.queryNullable(),
        LongController.queryOptional(),
        LongController.queryOptionalDefault(),
        LongController.queryList(),
        LongController.queryListDefault(),
        LongController.queryListNullable(),
        LongController.queryListOptional(),
        LongController.queryListOptionalDefault());
  }

  private static List<JavaScriptReverseRoute> doubleControllerRoutes() {
    return List.of(
        DoubleController.path(),
        DoubleController.query(),
        DoubleController.queryDefault(),
        DoubleController.queryFixed(),
        DoubleController.queryNullable(),
        DoubleController.queryOptional(),
        DoubleController.queryOptionalDefault(),
        DoubleController.queryList(),
        DoubleController.queryListDefault(),
        DoubleController.queryListNullable(),
        DoubleController.queryListOptional(),
        DoubleController.queryListOptionalDefault());
  }

  private static List<JavaScriptReverseRoute> floatControllerRoutes() {
    return List.of(
        FloatController.path(),
        FloatController.query(),
        FloatController.queryDefault(),
        FloatController.queryFixed(),
        FloatController.queryNullable(),
        FloatController.queryOptional(),
        FloatController.queryOptionalDefault(),
        FloatController.queryList(),
        FloatController.queryListDefault(),
        FloatController.queryListNullable(),
        FloatController.queryListOptional(),
        FloatController.queryListOptionalDefault());
  }

  private static List<JavaScriptReverseRoute> uuidControllerRoutes() {
    return List.of(
        UUIDController.path(),
        UUIDController.query(),
        UUIDController.queryDefault(),
        UUIDController.queryFixed(),
        UUIDController.queryNullable(),
        UUIDController.queryOptional(),
        UUIDController.queryOptionalDefault(),
        UUIDController.queryList(),
        UUIDController.queryListDefault(),
        UUIDController.queryListNullable(),
        UUIDController.queryListOptional(),
        UUIDController.queryListOptionalDefault());
  }

  private static List<JavaScriptReverseRoute> userControllerRoutes() {
    return List.of(
        UserController.path(),
        UserController.query(),
        UserController.queryDefault(),
        UserController.queryFixed(),
        UserController.queryNullable(),
        UserController.queryOptional(),
        UserController.queryOptionalDefault(),
        UserController.queryList(),
        UserController.queryListDefault(),
        UserController.queryListNullable(),
        UserController.queryListOptional(),
        UserController.queryListOptionalDefault());
  }

  private static List<JavaScriptReverseRoute> optionalControllerRoutes() {
    return List.of(
        OptionalController.queryInt(),
        OptionalController.queryIntDefault(),
        OptionalController.queryLong(),
        OptionalController.queryLongDefault(),
        OptionalController.queryDouble(),
        OptionalController.queryDoubleDefault());
  }

  private static List<JavaScriptReverseRoute> methodControllerRoutes() {
    return List.of(
        MethodController.get(),
        MethodController.post(),
        MethodController.put(),
        MethodController.patch(),
        MethodController.delete(),
        MethodController.head(),
        MethodController.options());
  }

  private static List<JavaScriptReverseRoute> assetsControllerRoutes() {
    return List.of(Assets.versioned());
  }

  private static List<JavaScriptReverseRoute> applicationControllerRoutes() {
    return List.of(Application.async(), Application.reverse(), Application.sameEscapedJavaIdentifier());
  }
}
