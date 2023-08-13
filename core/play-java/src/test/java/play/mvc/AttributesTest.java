/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import play.core.j.RequestHeaderImpl;
import play.libs.typedmap.TypedEntry;
import play.libs.typedmap.TypedKey;

public final class AttributesTest {

  static Stream<Arguments> targets() {
    return Stream.of(
        arguments(named("Java", new Http.RequestBuilder().build())),
        arguments(
            named("Scala", new RequestHeaderImpl(new Http.RequestBuilder().build().asScala()))));
  }

  @ParameterizedTest
  @MethodSource("targets")
  void testRequestHeader_addSingleAttribute(final Http.RequestHeader requestHeader) {
    final TypedKey<String> color = TypedKey.create("color");

    final Http.RequestHeader newRequestHeader = requestHeader.addAttr(color, "red");

    assertTrue(newRequestHeader.attrs().containsKey(color));
    assertEquals("red", newRequestHeader.attrs().get(color));
  }

  @ParameterizedTest
  @MethodSource("targets")
  void testRequestHeader_KeepCurrentAttributesWhenAddingANewOne(
      final Http.RequestHeader requestHeader) {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");

    Http.RequestHeader newRequestHeader = requestHeader.addAttr(color, "red").addAttr(number, 5L);

    assertTrue(newRequestHeader.attrs().containsKey(number));
    assertTrue(newRequestHeader.attrs().containsKey(color));
    assertEquals(((Long) 5L), newRequestHeader.attrs().get(number));
    assertEquals("red", newRequestHeader.attrs().get(color));
  }

  @ParameterizedTest
  @MethodSource("targets")
  void testRequestHeader_OverrideExistingValue(final Http.RequestHeader requestHeader) {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");

    Http.RequestHeader newRequestHeader =
        requestHeader.addAttr(color, "red").addAttr(number, 5L).addAttr(color, "white");

    assertTrue(newRequestHeader.attrs().containsKey(number));
    assertTrue(newRequestHeader.attrs().containsKey(color));
    assertEquals(((Long) 5L), newRequestHeader.attrs().get(number));
    assertEquals("white", newRequestHeader.attrs().get(color));
  }

  @ParameterizedTest
  @MethodSource("targets")
  void testRequestHeader_addMultipleAttributes(final Http.RequestHeader requestHeader) {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");

    final Http.RequestHeader newRequestHeader =
        requestHeader.addAttrs(new TypedEntry<>(color, "red"), new TypedEntry<>(number, 3L));

    assertTrue(newRequestHeader.attrs().containsKey(color));
    assertTrue(newRequestHeader.attrs().containsKey(number));
    assertEquals("red", newRequestHeader.attrs().get(color));
    assertEquals((Long) 3L, newRequestHeader.attrs().get(number));
  }

  @ParameterizedTest
  @MethodSource("targets")
  void testRequestHeader_KeepCurrentAttributesWhenAddingMultipleOnes(
      final Http.RequestHeader requestHeader) {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");
    final TypedKey<String> direction = TypedKey.create("direction");

    Http.RequestHeader newRequestHeader =
        requestHeader
            .addAttr(color, "red")
            .addAttrs(new TypedEntry<>(number, 5L), new TypedEntry<>(direction, "left"));

    assertTrue(newRequestHeader.attrs().containsKey(number));
    assertTrue(newRequestHeader.attrs().containsKey(direction));
    assertTrue(newRequestHeader.attrs().containsKey(color));
    assertEquals(((Long) 5L), newRequestHeader.attrs().get(number));
    assertEquals("left", newRequestHeader.attrs().get(direction));
    assertEquals("red", newRequestHeader.attrs().get(color));
  }

  @ParameterizedTest
  @MethodSource("targets")
  void testRequestHeader_OverrideExistingValueWhenAddingMultipleAttributes(
      final Http.RequestHeader requestHeader) {
    final TypedKey<Long> number = TypedKey.create("number");
    final TypedKey<String> color = TypedKey.create("color");

    Http.RequestHeader newRequestHeader =
        requestHeader
            .addAttr(color, "red")
            .addAttrs(new TypedEntry<>(number, 5L), new TypedEntry<>(color, "white"));

    assertTrue(newRequestHeader.attrs().containsKey(number));
    assertTrue(newRequestHeader.attrs().containsKey(color));
    assertEquals(((Long) 5L), newRequestHeader.attrs().get(number));
    assertEquals("white", newRequestHeader.attrs().get(color));
  }
}
