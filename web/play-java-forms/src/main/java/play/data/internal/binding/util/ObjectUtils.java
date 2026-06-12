/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Modified from the original Spring Framework source for Play Framework form binding by the Play Framework contributors.
 */

package play.data.internal.binding.util;

import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.TimeZone;

/**
 * Miscellaneous object utility methods.
 *
 * <p>Mainly for internal use within the framework.
 *
 * <p>Thanks to Alex Ruiz for contributing several enhancements to this class!
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Chris Beams
 * @author Sam Brannen
 * @see ClassUtils
 * @see CollectionUtils
 * @see StringUtils
 */
public abstract class ObjectUtils {

	private static final String EMPTY_STRING = "";
	private static final String NULL_STRING = "null";
	private static final String ARRAY_START = "{";
	private static final String ARRAY_END = "}";
	private static final String EMPTY_ARRAY = ARRAY_START + ARRAY_END;
	private static final String ARRAY_ELEMENT_SEPARATOR = ", ";
	private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
	private static final String NON_EMPTY_ARRAY = ARRAY_START + "..." + ARRAY_END;
	private static final String COLLECTION = "[...]";
	private static final String MAP = NON_EMPTY_ARRAY;


	/**
	 * Determine whether the given object is an array:
	 * either an Object array or a primitive array.
	 * @param obj the object to check
	 */
	public static boolean isArray(Object obj) {
		return (obj != null && obj.getClass().isArray());
	}

	/**
	 * Determine whether the given array is empty:
	 * i.e. {@code null} or of zero length.
	 * @param array the array to check
	 */
	public static boolean isEmpty(Object [] array) {
		return (array == null || array.length == 0);
	}

	/**
	 * Unwrap the given object which is potentially a {@link java.util.Optional}.
	 * @param obj the candidate object
	 * @return either the value held within the {@code Optional}, {@code null}
	 * if the {@code Optional} is empty, or simply the given object as-is
	 */
	public static Object unwrapOptional(Object obj) {
		if (obj instanceof Optional<?> optional) {
			Object result = optional.orElse(null);
			Assert.isTrue(!(result instanceof Optional), "Multi-level Optional usage not supported");
			return result;
		}
		return obj;
	}

	/**
	 * Convert the given array (which may be a primitive array) to an object array (if
	 * necessary, to an array of primitive wrapper objects).
	 * <p>A {@code null} source value or empty primitive array will be converted to an
	 * empty Object array.
	 * @param source the (potentially primitive) array
	 * @return the corresponding object array (never {@code null})
	 * @throws IllegalArgumentException if the parameter is not an array
	 */
	public static Object[] toObjectArray(Object source) {
		if (source instanceof Object[] objects) {
			return objects;
		}
		if (source == null) {
			return EMPTY_OBJECT_ARRAY;
		}
		if (!source.getClass().isArray()) {
			throw new IllegalArgumentException("Source is not an array: " + source);
		}
		int length = Array.getLength(source);
		if (length == 0) {
			return EMPTY_OBJECT_ARRAY;
		}
		Class<?> wrapperType = Array.get(source, 0).getClass();
		Object[] newArray = (Object[]) Array.newInstance(wrapperType, length);
		for (int i = 0; i < length; i++) {
			newArray[i] = Array.get(source, i);
		}
		return newArray;
	}


	//---------------------------------------------------------------------
	// Convenience methods for content-based equality/hash-code handling
	//---------------------------------------------------------------------

	/**
	 * Determine if the given objects are equal, returning {@code true} if
	 * both are {@code null} or {@code false} if only one is {@code null}.
	 * <p>Compares arrays with {@code Arrays.equals}, performing an equality
	 * check based on the array elements rather than the array reference.
	 * @param o1 first Object to compare
	 * @param o2 second Object to compare
	 * @return whether the given objects are equal
	 * @see Object#equals(Object)
	 * @see java.util.Arrays#equals
	 */
	public static boolean nullSafeEquals(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		if (o1.equals(o2)) {
			return true;
		}
		if (o1.getClass().isArray() && o2.getClass().isArray()) {
			return arrayEquals(o1, o2);
		}
		return false;
	}

	/**
	 * Compare the given arrays with {@code Arrays.equals(x, y)} variants, performing
	 * an equality check based on the array elements rather than the array reference.
	 * @param o1 first array to compare
	 * @param o2 second array to compare
	 * @return whether the given objects are equal
	 * @see #nullSafeEquals(Object, Object)
	 * @see java.util.Arrays
	 */
	private static boolean arrayEquals(Object o1, Object o2) {
		if (o1 instanceof Object[] objects1 && o2 instanceof Object[] objects2) {
			return Arrays.equals(objects1, objects2);
		}
		Class<?> type1 = o1.getClass();
		Class<?> type2 = o2.getClass();
		if (type1 == boolean[].class && type2 == boolean[].class) {
			return Arrays.equals((boolean[]) o1, (boolean[]) o2);
		}
		if (type1 == byte[].class && type2 == byte[].class) {
			return Arrays.equals((byte[]) o1, (byte[]) o2);
		}
		if (type1 == char[].class && type2 == char[].class) {
			return Arrays.equals((char[]) o1, (char[]) o2);
		}
		if (type1 == double[].class && type2 == double[].class) {
			return Arrays.equals((double[]) o1, (double[]) o2);
		}
		if (type1 == float[].class && type2 == float[].class) {
			return Arrays.equals((float[]) o1, (float[]) o2);
		}
		if (type1 == int[].class && type2 == int[].class) {
			return Arrays.equals((int[]) o1, (int[]) o2);
		}
		if (type1 == long[].class && type2 == long[].class) {
			return Arrays.equals((long[]) o1, (long[]) o2);
		}
		if (type1 == short[].class && type2 == short[].class) {
			return Arrays.equals((short[]) o1, (short[]) o2);
		}
		return false;
	}

	/**
	 * Return a hash code for the given elements, delegating to
	 * {@link #nullSafeHashCode(Object)} for each element. Contrary
	 * to {@link Objects#hash(Object...)}, this method can handle an
	 * element that is an array.
	 * @param elements the elements to be hashed
	 * @return a hash value of the elements
	 */
	public static int nullSafeHash(Object ... elements) {
		if (elements == null) {
			return 0;
		}
		int result = 1;
		for (Object element : elements) {
			result = 31 * result + nullSafeHashCode(element);
		}
		return result;
	}

	/**
	 * Return a hash code for the given object, typically the value of
	 * {@link Object#hashCode()}. If the object is an array, this method
	 * will delegate to one of the {@code Arrays.hashCode} methods. If
	 * the object is {@code null}, this method returns {@code 0}.
	 * @see Object#hashCode()
	 * @see Arrays
	 */
	public static int nullSafeHashCode(Object obj) {
		if (obj == null) {
			return 0;
		}
		if (obj.getClass().isArray()) {
			if (obj instanceof Object[] objects) {
				return Arrays.hashCode(objects);
			}
			if (obj instanceof boolean[] booleans) {
				return Arrays.hashCode(booleans);
			}
			if (obj instanceof byte[] bytes) {
				return Arrays.hashCode(bytes);
			}
			if (obj instanceof char[] chars) {
				return Arrays.hashCode(chars);
			}
			if (obj instanceof double[] doubles) {
				return Arrays.hashCode(doubles);
			}
			if (obj instanceof float[] floats) {
				return Arrays.hashCode(floats);
			}
			if (obj instanceof int[] ints) {
				return Arrays.hashCode(ints);
			}
			if (obj instanceof long[] longs) {
				return Arrays.hashCode(longs);
			}
			if (obj instanceof short[] shorts) {
				return Arrays.hashCode(shorts);
			}
		}
		return obj.hashCode();
	}


	//---------------------------------------------------------------------
	// Convenience methods for toString output
	//---------------------------------------------------------------------

	/**
	 * Return a String representation of an object's overall identity.
	 * @param obj the object (may be {@code null})
	 * @return the object's identity as String representation,
	 * or an empty String if the object was {@code null}
	 */
	public static String identityToString(Object obj) {
		if (obj == null) {
			return EMPTY_STRING;
		}
		return obj.getClass().getName() + "@" + getIdentityHexString(obj);
	}

	/**
	 * Return a hex String form of an object's identity hash code.
	 * @param obj the object
	 * @return the object's identity code in hex notation
	 */
	public static String getIdentityHexString(Object obj) {
		return Integer.toHexString(System.identityHashCode(obj));
	}

	/**
	 * Return a String representation of the specified Object.
	 * <p>Builds a String representation of the contents in case of an array.
	 * Returns a {@code "null"} String if {@code obj} is {@code null}.
	 * @param obj the object to build a String representation for
	 * @return a String representation of {@code obj}
	 * @see #nullSafeConciseToString(Object)
	 */
	public static String nullSafeToString(Object obj) {
		if (obj == null) {
			return NULL_STRING;
		}
		if (obj instanceof String string) {
			return string;
		}
		if (obj instanceof Object[] objects) {
			return nullSafeToString(objects);
		}
		if (obj instanceof boolean[] booleans) {
			return nullSafeToString(booleans);
		}
		if (obj instanceof byte[] bytes) {
			return nullSafeToString(bytes);
		}
		if (obj instanceof char[] chars) {
			return nullSafeToString(chars);
		}
		if (obj instanceof double[] doubles) {
			return nullSafeToString(doubles);
		}
		if (obj instanceof float[] floats) {
			return nullSafeToString(floats);
		}
		if (obj instanceof int[] ints) {
			return nullSafeToString(ints);
		}
		if (obj instanceof long[] longs) {
			return nullSafeToString(longs);
		}
		if (obj instanceof short[] shorts) {
			return nullSafeToString(shorts);
		}
		String str = obj.toString();
		return (str != null ? str : EMPTY_STRING);
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>The String representation consists of a list of the array's elements,
	 * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
	 * by the characters {@code ", "} (a comma followed by a space).
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(Object [] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (Object o : array) {
			stringJoiner.add(String.valueOf(o));
		}
		return stringJoiner.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>The String representation consists of a list of the array's elements,
	 * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
	 * by the characters {@code ", "} (a comma followed by a space).
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(boolean [] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (boolean b : array) {
			stringJoiner.add(String.valueOf(b));
		}
		return stringJoiner.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>As of 7.0, the String representation is a hex-encoded string enclosed
	 * in curly braces ({@code "{}"}). The String consists of 2 hexadecimal
	 * chars per element, and without a delimiter between adjacent elements.
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(byte [] array) {
		if (array == null) {
			return NULL_STRING;
		}
		if (array.length == 0) {
			return EMPTY_ARRAY;
		}
		return ARRAY_START + HexFormat.of().formatHex(array) + ARRAY_END;
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>The String representation consists of a list of the array's elements,
	 * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
	 * by the characters {@code ", "} (a comma followed by a space).
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(char [] array) {
		if (array == null) {
			return NULL_STRING;
		}
		if (array.length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (char c : array) {
			stringJoiner.add('\'' + String.valueOf(c) + '\'');
		}
		return stringJoiner.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>The String representation consists of a list of the array's elements,
	 * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
	 * by the characters {@code ", "} (a comma followed by a space).
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(double [] array) {
		if (array == null) {
			return NULL_STRING;
		}
		if (array.length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (double d : array) {
			stringJoiner.add(String.valueOf(d));
		}
		return stringJoiner.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>The String representation consists of a list of the array's elements,
	 * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
	 * by the characters {@code ", "} (a comma followed by a space).
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(float [] array) {
		if (array == null) {
			return NULL_STRING;
		}
		if (array.length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (float f : array) {
			stringJoiner.add(String.valueOf(f));
		}
		return stringJoiner.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>The String representation consists of a list of the array's elements,
	 * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
	 * by the characters {@code ", "} (a comma followed by a space).
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(int [] array) {
		if (array == null) {
			return NULL_STRING;
		}
		if (array.length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (int i : array) {
			stringJoiner.add(String.valueOf(i));
		}
		return stringJoiner.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>The String representation consists of a list of the array's elements,
	 * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
	 * by the characters {@code ", "} (a comma followed by a space).
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(long [] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (long l : array) {
			stringJoiner.add(String.valueOf(l));
		}
		return stringJoiner.toString();
	}

	/**
	 * Return a String representation of the contents of the specified array.
	 * <p>The String representation consists of a list of the array's elements,
	 * enclosed in curly braces ({@code "{}"}). Adjacent elements are separated
	 * by the characters {@code ", "} (a comma followed by a space).
	 * Returns a {@code "null"} String if {@code array} is {@code null}.
	 * @param array the array to build a String representation for
	 * @return a String representation of {@code array}
	 */
	public static String nullSafeToString(short [] array) {
		if (array == null) {
			return NULL_STRING;
		}
		if (array.length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (short s : array) {
			stringJoiner.add(String.valueOf(s));
		}
		return stringJoiner.toString();
	}

	/**
	 * Generate a null-safe, concise string representation of the supplied object
	 * as described below.
	 * <p>Favor this method over {@link #nullSafeToString(Object)} when you need
	 * the length of the generated string to be limited.
	 * <p>Returns:
	 * <ul>
	 * <li>{@code "null"} if {@code obj} is {@code null}</li>
	 * <li>{@code "Optional.empty"} if {@code obj} is an empty {@link Optional}</li>
	 * <li>{@code "Optional[<concise-string>]"} if {@code obj} is a non-empty {@code Optional},
	 * where {@code <concise-string>} is the result of invoking this method on the object
	 * contained in the {@code Optional}</li>
	 * <li>{@code "{}"} if {@code obj} is an empty array</li>
	 * <li>{@code "{...}"} if {@code obj} is a {@link Map} or a non-empty array</li>
	 * <li>{@code "[...]"} if {@code obj} is a {@link Collection}</li>
	 * <li>{@linkplain Class#getName() Class name} if {@code obj} is a {@link Class}</li>
	 * <li>{@linkplain Charset#name() Charset name} if {@code obj} is a {@link Charset}</li>
	 * <li>{@linkplain TimeZone#getID() TimeZone ID} if {@code obj} is a {@link TimeZone}</li>
	 * <li>{@linkplain ZoneId#getId() Zone ID} if {@code obj} is a {@link ZoneId}</li>
	 * <li>Potentially {@linkplain StringUtils#truncate(CharSequence) truncated string}
	 * if {@code obj} is a {@link String} or {@link CharSequence}</li>
	 * <li>Potentially {@linkplain StringUtils#truncate(CharSequence) truncated string}
	 * if {@code obj} is a <em>simple value type</em> whose {@code toString()} method
	 * returns a non-null value</li>
	 * <li>Otherwise, a string representation of the object's type name concatenated
	 * with {@code "@"} and a hex string form of the object's identity hash code</li>
	 * </ul>
	 * <p>In the context of this method, a <em>simple value type</em> is any of the following:
	 * primitive wrapper (excluding {@link Void}), {@link Enum}, {@link Number},
	 * {@link java.util.Date Date}, {@link java.time.temporal.Temporal Temporal},
	 * {@link java.io.File File}, {@link java.nio.file.Path Path},
	 * {@link java.net.URI URI}, {@link java.net.URL URL},
	 * {@link java.net.InetAddress InetAddress}, {@link java.util.Currency Currency},
	 * {@link java.util.Locale Locale}, {@link java.util.UUID UUID},
	 * {@link java.util.regex.Pattern Pattern}.
	 * @param obj the object to build a string representation for
	 * @return a concise string representation of the supplied object
	 * @see #nullSafeToString(Object)
	 * @see StringUtils#truncate(CharSequence)
	 * @see ClassUtils#isSimpleValueType(Class)
	 */
	public static String nullSafeConciseToString(Object obj) {
		if (obj == null) {
			return NULL_STRING;
		}
		if (obj instanceof Optional<?> optional) {
			return (optional.isEmpty() ? "Optional.empty" :
				"Optional[%s]".formatted(nullSafeConciseToString(optional.get())));
		}
		if (obj.getClass().isArray()) {
			return (Array.getLength(obj) == 0 ? EMPTY_ARRAY : NON_EMPTY_ARRAY);
		}
		if (obj instanceof Collection) {
			return COLLECTION;
		}
		if (obj instanceof Map) {
			return MAP;
		}
		if (obj instanceof Class<?> clazz) {
			return clazz.getName();
		}
		if (obj instanceof Charset charset) {
			return charset.name();
		}
		if (obj instanceof TimeZone timeZone) {
			return timeZone.getID();
		}
		if (obj instanceof ZoneId zoneId) {
			return zoneId.getId();
		}
		if (obj instanceof CharSequence charSequence) {
			return StringUtils.truncate(charSequence);
		}
		Class<?> type = obj.getClass();
		if (ClassUtils.isSimpleValueType(type)) {
			String str = obj.toString();
			if (str != null) {
				return StringUtils.truncate(str);
			}
		}
		return type.getTypeName() + "@" + getIdentityHexString(obj);
	}

}
