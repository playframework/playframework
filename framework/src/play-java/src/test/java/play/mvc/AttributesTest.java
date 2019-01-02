/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import play.core.j.RequestHeaderImpl;
import play.libs.typedmap.TypedKey;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public final class AttributesTest {

	@Parameters
	public static Collection<Http.RequestHeader> targets() {
		return Arrays.asList(new Http.RequestBuilder().build(),
				new RequestHeaderImpl(new Http.RequestBuilder().build().asScala()));
	}

	private Http.RequestHeader requestHeader;

	public AttributesTest(final Http.RequestHeader requestHeader) {
		this.requestHeader = requestHeader;
	}

	@Test
	public void testRequestHeader_addSingleAttribute() {
		final TypedKey<String> color = TypedKey.create("color");

		final Http.RequestHeader newRequestHeader = requestHeader.addAttr(color, "red");

		assertTrue(newRequestHeader.attrs().containsKey(color));
		assertEquals("red", newRequestHeader.attrs().get(color));
	}

	@Test
	public void testRequestHeader_KeepCurrentAttributesWhenAddingANewOne() {
		final TypedKey<Long> number = TypedKey.create("number");
		final TypedKey<String> color = TypedKey.create("color");

		Http.RequestHeader newRequestHeader = requestHeader.addAttr(color, "red")
				.addAttr(number, 5L);

		assertTrue(newRequestHeader.attrs().containsKey(number));
		assertTrue(newRequestHeader.attrs().containsKey(color));
		assertEquals(((Long) 5L), newRequestHeader.attrs().get(number));
		assertEquals("red", newRequestHeader.attrs().get(color));
	}

	@Test
	public void testRequestHeader_OverrideExistingValue() {
		final TypedKey<Long> number = TypedKey.create("number");
		final TypedKey<String> color = TypedKey.create("color");

		Http.RequestHeader newRequestHeader = requestHeader
				.addAttr(color, "red")
				.addAttr(number, 5L)
				.addAttr(color, "white");

		assertTrue(newRequestHeader.attrs().containsKey(number));
		assertTrue(newRequestHeader.attrs().containsKey(color));
		assertEquals(((Long) 5L), newRequestHeader.attrs().get(number));
		assertEquals("white", newRequestHeader.attrs().get(color));
	}

}
