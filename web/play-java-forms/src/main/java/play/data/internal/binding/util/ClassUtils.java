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

import java.io.Closeable;
import java.io.Externalizable;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Miscellaneous {@code java.lang.Class} utility methods.
 *
 * <p>Mainly for internal use within the framework.
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rob Harrop
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Sungbin Yang
 */
public abstract class ClassUtils {

	/** Suffix for array class names: {@code "[]"}. */
	public static final String ARRAY_SUFFIX = "[]";

	/** A reusable empty class array constant. */
	private static final Class<?>[] EMPTY_CLASS_ARRAY = {};

	/** The package separator character: {@code '.'}. */
	private static final char PACKAGE_SEPARATOR = '.';

	/** The nested class separator character: {@code '$'}. */
	private static final char NESTED_CLASS_SEPARATOR = '$';

	/** The CGLIB class separator: {@code "$$"}. */
	public static final String CGLIB_CLASS_SEPARATOR = "$$";

	/**
	 * Map with primitive wrapper type as key and corresponding primitive
	 * type as value, for example: {@code Integer.class -> int.class}.
	 */
	private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<>(9);

	/**
	 * Map with primitive type as key and corresponding wrapper
	 * type as value, for example: {@code int.class -> Integer.class}.
	 */
	private static final Map<Class<?>, Class<?>> primitiveTypeToWrapperMap = new IdentityHashMap<>(9);

	/**
	 * Map with primitive type name as key and corresponding primitive
	 * type as value, for example: {@code "int" -> int.class}.
	 */
	private static final Map<String, Class<?>> primitiveTypeNameMap = new HashMap<>(32);

	/**
	 * Map with common Java language class name as key and corresponding Class as value.
	 * Primarily for efficient deserialization of remote invocations.
	 */
	private static final Map<String, Class<?>> commonClassCache = new HashMap<>(64);

	/**
	 * Common Java language interfaces which are supposed to be ignored
	 * when searching for 'primary' user-level interfaces.
	 */
	private static final Set<Class<?>> javaLanguageInterfaces;

	/**
	 * Cache for equivalent methods on a interface implemented by the declaring class.
	 * <p>A {@code null} value signals that no interface method was found for the key.
	 */
	static {
		primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
		primitiveWrapperTypeMap.put(Byte.class, byte.class);
		primitiveWrapperTypeMap.put(Character.class, char.class);
		primitiveWrapperTypeMap.put(Double.class, double.class);
		primitiveWrapperTypeMap.put(Float.class, float.class);
		primitiveWrapperTypeMap.put(Integer.class, int.class);
		primitiveWrapperTypeMap.put(Long.class, long.class);
		primitiveWrapperTypeMap.put(Short.class, short.class);
		primitiveWrapperTypeMap.put(Void.class, void.class);

		// Map entry iteration is less expensive to initialize than forEach with lambdas
		for (Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperTypeMap.entrySet()) {
			primitiveTypeToWrapperMap.put(entry.getValue(), entry.getKey());
			registerCommonClasses(entry.getKey());
		}

		Set<Class<?>> primitiveTypes = new HashSet<>(32);
		primitiveTypes.addAll(primitiveWrapperTypeMap.values());
		Collections.addAll(primitiveTypes, boolean[].class, byte[].class, char[].class,
				double[].class, float[].class, int[].class, long[].class, short[].class);
		for (Class<?> primitiveType : primitiveTypes) {
			primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
		}

		registerCommonClasses(Boolean[].class, Byte[].class, Character[].class, Double[].class,
				Float[].class, Integer[].class, Long[].class, Short[].class);
		registerCommonClasses(Number.class, Number[].class, String.class, String[].class,
				Class.class, Class[].class, Object.class, Object[].class);
		registerCommonClasses(Throwable.class, Exception.class, RuntimeException.class,
				Error.class, StackTraceElement.class, StackTraceElement[].class);
		registerCommonClasses(Enum.class, Iterable.class, Iterator.class, Enumeration.class,
				Collection.class, List.class, Set.class, Map.class, Map.Entry.class, Optional.class);

		Class<?>[] javaLanguageInterfaceArray = {Serializable.class, Externalizable.class,
				Closeable.class, AutoCloseable.class, Cloneable.class, Comparable.class};
		registerCommonClasses(javaLanguageInterfaceArray);
		javaLanguageInterfaces = Set.of(javaLanguageInterfaceArray);
	}


	/**
	 * Register the given common classes with the ClassUtils cache.
	 */
	private static void registerCommonClasses(Class<?>... commonClasses) {
		for (Class<?> clazz : commonClasses) {
			commonClassCache.put(clazz.getName(), clazz);
		}
	}

	/**
	 * Return the default ClassLoader to use: typically the thread context
	 * ClassLoader, if available; the ClassLoader that loaded the ClassUtils
	 * class will be used as fallback.
	 * <p>Call this method if you intend to use the thread context ClassLoader
	 * in a scenario where you clearly prefer a non-null ClassLoader reference:
	 * for example, for class path resource loading (but not necessarily for
	 * {@code Class.forName}, which accepts a {@code null} ClassLoader
	 * reference as well).
	 * @return the default ClassLoader (only {@code null} if even the system
	 * ClassLoader isn't accessible)
	 * @see Thread#getContextClassLoader()
	 * @see ClassLoader#getSystemClassLoader()
	 */
	public static ClassLoader getDefaultClassLoader() {
		ClassLoader cl = null;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		}
		catch (Throwable ex) {
			// Cannot access thread context ClassLoader - falling back...
		}
		if (cl == null) {
			// No thread context class loader -> use class loader of this class.
			cl = ClassUtils.class.getClassLoader();
			if (cl == null) {
				// getClassLoader() returning null indicates the bootstrap ClassLoader
				try {
					cl = ClassLoader.getSystemClassLoader();
				}
				catch (Throwable ex) {
					// Cannot access system ClassLoader - oh well, maybe the caller can live with null...
				}
			}
		}
		return cl;
	}

	/**
	 * Replacement for {@code Class.forName()} that also returns Class instances
	 * for primitives (for example, "int") and array class names (for example, "String[]").
	 * Furthermore, it is also capable of resolving nested class names in Java source
	 * style (for example, "java.lang.Thread.State" instead of "java.lang.Thread$State").
	 * @param name the name of the Class
	 * @param classLoader the class loader to use
	 * (can be {@code null}, which indicates the default class loader)
	 * @return a class instance for the supplied name
	 * @throws ClassNotFoundException if the class was not found
	 * @throws LinkageError if the class file could not be loaded
	 * @see Class#forName(String, boolean, ClassLoader)
	 */
	public static Class<?> forName(String name, ClassLoader classLoader)
			throws ClassNotFoundException, LinkageError {

		Assert.notNull(name, "Name must not be null");

		Class<?> clazz = resolvePrimitiveClassName(name);
		if (clazz == null) {
			clazz = commonClassCache.get(name);
		}
		if (clazz != null) {
			return clazz;
		}

		// "java.lang.String[]" style arrays
		if (name.endsWith(ARRAY_SUFFIX)) {
			String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
			Class<?> elementClass = forName(elementClassName, classLoader);
			return elementClass.arrayType();
		}

		ClassLoader clToUse = classLoader;
		if (clToUse == null) {
			clToUse = getDefaultClassLoader();
		}
		try {
			return Class.forName(name, false, clToUse);
		}
		catch (ClassNotFoundException ex) {
			int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
			int previousDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR, lastDotIndex - 1);
			if (lastDotIndex != -1 && previousDotIndex != -1 && Character.isUpperCase(name.charAt(previousDotIndex + 1))) {
				String nestedClassName =
						name.substring(0, lastDotIndex) + NESTED_CLASS_SEPARATOR + name.substring(lastDotIndex + 1);
				try {
					return Class.forName(nestedClassName, false, clToUse);
				}
				catch (ClassNotFoundException ex2) {
					// Swallow - let original exception get through
				}
			}
			throw ex;
		}
	}

	/**
	 * Check whether the given class is cache-safe in the given context,
	 * i.e. whether it is loaded by the given ClassLoader or a parent of it.
	 * @param clazz the class to analyze
	 * @param classLoader the ClassLoader to potentially cache metadata in
	 * (can be {@code null} which indicates the system class loader)
	 */
	public static boolean isCacheSafe(Class<?> clazz, ClassLoader classLoader) {
		Assert.notNull(clazz, "Class must not be null");
		try {
			ClassLoader target = clazz.getClassLoader();
			// Common cases
			if (target == classLoader || target == null) {
				return true;
			}
			if (classLoader == null) {
				return false;
			}
			// Check for match in ancestors -> positive
			ClassLoader current = classLoader;
			while (current != null) {
				current = current.getParent();
				if (current == target) {
					return true;
				}
			}
			// Check for match in children -> negative
			while (target != null) {
				target = target.getParent();
				if (target == classLoader) {
					return false;
				}
			}
		}
		catch (SecurityException ex) {
			return false;
		}

		return false;
	}

	/**
	 * Resolve the given class name as primitive class, if appropriate,
	 * according to the JVM's naming rules for primitive classes.
	 * <p>Also supports the JVM's internal class names for primitive arrays.
	 * Does <i>not</i> support the "[]" suffix notation for primitive arrays;
	 * this is only supported by {@link #forName(String, ClassLoader)}.
	 * @param name the name of the potentially primitive class
	 * @return the primitive class, or {@code null} if the name does not denote
	 * a primitive class or primitive array class
	 */
	public static Class<?> resolvePrimitiveClassName(String name) {
		Class<?> result = null;
		// Most class names will be quite long, considering that they
		// SHOULD sit in a package, so a length check is worthwhile.
		if (name != null && name.length() <= 7) {
			// Could be a primitive - likely.
			result = primitiveTypeNameMap.get(name);
		}
		return result;
	}

	/**
	 * Check if the given class represents a primitive wrapper,
	 * i.e. Boolean, Byte, Character, Short, Integer, Long, Float, Double, or
	 * Void.
	 * @param clazz the class to check
	 * @return whether the given class is a primitive wrapper class
	 */
	public static boolean isPrimitiveWrapper(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return primitiveWrapperTypeMap.containsKey(clazz);
	}

	/**
	 * Check if the given class represents a primitive (i.e. boolean, byte,
	 * char, short, int, long, float, or double), {@code void}, or a wrapper for
	 * those types (i.e. Boolean, Byte, Character, Short, Integer, Long, Float,
	 * Double, or Void).
	 * @param clazz the class to check
	 * @return {@code true} if the given class represents a primitive, void, or
	 * a wrapper class
	 */
	public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
	}

	/**
	 * Resolve the given class if it is a primitive class,
	 * returning the corresponding primitive wrapper type instead.
	 * @param clazz the class to check
	 * @return the original class, or a primitive wrapper for the original primitive type
	 */
	@SuppressWarnings("NullAway") // Dataflow analysis limitation
	public static Class<?> resolvePrimitiveIfNecessary(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isPrimitive() && clazz != void.class ? primitiveTypeToWrapperMap.get(clazz) : clazz);
	}

	/**
	 * Determine if the given type represents either {@code Void} or {@code void}.
	 * @param type the type to check
	 * @return {@code true} if the type represents {@code Void} or {@code void}
	 * @see Void
	 * @see Void#TYPE
	 */
	public static boolean isVoidType(Class<?> type) {
		return (type == void.class || type == Void.class);
	}

	/**
	 * Also used by {@link ObjectUtils#nullSafeConciseToString}.
	 * <p>Check if the given type represents a common "simple" value type:
	 * primitive or primitive wrapper, {@link Enum}, {@link String} or other
	 * {@link CharSequence}, {@link Number}, {@link Date}, {@link Temporal},
	 * {@link ZoneId}, {@link TimeZone}, {@link File}, {@link Path}, {@link URI},
	 * {@link URL}, {@link InetAddress}, {@link Charset}, {@link Currency},
	 * {@link Locale}, {@link UUID}, {@link Pattern}, or {@link Class}.
	 * <p>{@code Void} and {@code void} are not considered simple value types.
	 * @param type the type to check
	 * @return whether the given type represents a "simple" value type,
	 * suggesting value-based data binding and {@code toString} output
	 */
	public static boolean isSimpleValueType(Class<?> type) {
		return (!isVoidType(type) &&
				(isPrimitiveOrWrapper(type) ||
				Enum.class.isAssignableFrom(type) ||
				CharSequence.class.isAssignableFrom(type) ||
				Number.class.isAssignableFrom(type) ||
				Date.class.isAssignableFrom(type) ||
				Temporal.class.isAssignableFrom(type) ||
				ZoneId.class.isAssignableFrom(type) ||
				TimeZone.class.isAssignableFrom(type) ||
				File.class.isAssignableFrom(type) ||
				Path.class.isAssignableFrom(type) ||
				Charset.class.isAssignableFrom(type) ||
				Currency.class.isAssignableFrom(type) ||
				InetAddress.class.isAssignableFrom(type) ||
				URI.class == type ||
				URL.class == type ||
				UUID.class == type ||
				Locale.class == type ||
				Pattern.class == type ||
				Class.class == type));
	}

	/**
	 * Check if the right-hand side type may be assigned to the left-hand side
	 * type, assuming setting by reflection. Considers primitive wrapper
	 * classes as assignable to the corresponding primitive types.
	 * @param lhsType the target type (left-hand side (LHS) type)
	 * @param rhsType the value type (right-hand side (RHS) type) that should
	 * be assigned to the target type
	 * @return {@code true} if {@code rhsType} is assignable to {@code lhsType}
	 */
	public static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
		Assert.notNull(lhsType, "Left-hand side type must not be null");
		Assert.notNull(rhsType, "Right-hand side type must not be null");
		if (lhsType.isAssignableFrom(rhsType)) {
			return true;
		}
		if (lhsType.isPrimitive()) {
			Class<?> resolvedPrimitive = primitiveWrapperTypeMap.get(rhsType);
			return (lhsType == resolvedPrimitive);
		}
		else if (rhsType.isPrimitive()) {
			Class<?> resolvedWrapper = primitiveTypeToWrapperMap.get(rhsType);
			return (resolvedWrapper != null && lhsType.isAssignableFrom(resolvedWrapper));
		}
		return false;
	}

	/**
	 * Determine if the given type is assignable from the given value,
	 * assuming setting by reflection. Considers primitive wrapper classes
	 * as assignable to the corresponding primitive types.
	 * @param type the target type
	 * @param value the value that should be assigned to the type
	 * @return if the type is assignable from the value
	 */
	public static boolean isAssignableValue(Class<?> type, Object value) {
		Assert.notNull(type, "Type must not be null");
		return (value != null ? isAssignable(type, value.getClass()) : !type.isPrimitive());
	}

	/**
	 * Copy the given {@code Collection} into a {@code Class} array.
	 * <p>The {@code Collection} must contain {@code Class} elements only.
	 * @param collection the {@code Collection} to copy
	 * @return the {@code Class} array
	 * @see StringUtils#toStringArray
	 */
	public static Class<?>[] toClassArray(Collection<Class<?>> collection) {
		return (!CollectionUtils.isEmpty(collection) ? collection.toArray(EMPTY_CLASS_ARRAY) : EMPTY_CLASS_ARRAY);
	}

	/**
	 * Return all interfaces that the given class implements as an array,
	 * including ones implemented by superclasses.
	 * <p>If the class itself is an interface, it gets returned as sole interface.
	 * @param clazz the class to analyze for interfaces
	 * @return all interfaces that the given object implements as an array
	 */
	public static Class<?>[] getAllInterfacesForClass(Class<?> clazz) {
		return getAllInterfacesForClass(clazz, null);
	}

	public static Class<?>[] getAllInterfacesForClass(Class<?> clazz, ClassLoader classLoader) {
		return toClassArray(getAllInterfacesForClassAsSet(clazz, classLoader));
	}

	public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz, ClassLoader classLoader) {
		Assert.notNull(clazz, "Class must not be null");
		if (clazz.isInterface()) {
			return Collections.singleton(clazz);
		}
		Set<Class<?>> interfaces = new LinkedHashSet<>();
		Class<?> current = clazz;
		while (current != null) {
			Class<?>[] ifcs = current.getInterfaces();
			for (Class<?> ifc : ifcs) {
					interfaces.add(ifc);
			}
			current = current.getSuperclass();
		}
		return interfaces;
	}

	/**
	 * Determine whether the given interface is a common Java language interface:
	 * {@link Serializable}, {@link Externalizable}, {@link Closeable}, {@link AutoCloseable},
	 * {@link Cloneable}, {@link Comparable} - all of which can be ignored when looking
	 * for 'primary' user-level interfaces. Common characteristics: no service-level
	 * operations, no bean property methods, no default methods.
	 * @param ifc the interface to check
	 */
	public static boolean isJavaLanguageInterface(Class<?> ifc) {
		return javaLanguageInterfaces.contains(ifc);
	}

	/**
	 * Return the user-defined class for the given class: usually simply the given
	 * class, but the original class in case of a CGLIB-generated subclass.
	 * @param clazz the class to check
	 * @return the user-defined class
	 * @see #CGLIB_CLASS_SEPARATOR
	 */
	public static Class<?> getUserClass(Class<?> clazz) {
		if (clazz.getName().contains(CGLIB_CLASS_SEPARATOR)) {
			Class<?> superclass = clazz.getSuperclass();
			if (superclass != null && superclass != Object.class) {
				return superclass;
			}
		}
		return clazz;
	}

	/**
	 * Return a descriptive name for the given object's type: usually simply
	 * the class name, but component type class name + "[]" for arrays,
	 * and an appended list of implemented interfaces for JDK proxies.
	 * @param value the value to introspect
	 * @return the qualified name of the class
	 */
	public static String getDescriptiveType(Object value) {
		if (value == null) {
			return null;
		}
		Class<?> clazz = value.getClass();
		if (Proxy.isProxyClass(clazz)) {
			String prefix = clazz.getTypeName() + " implementing ";
			StringJoiner result = new StringJoiner(",", prefix, "");
			for (Class<?> ifc : clazz.getInterfaces()) {
				result.add(ifc.getTypeName());
			}
			return result.toString();
		}
		else {
			return clazz.getTypeName();
		}
	}

	public static String getCanonicalName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		String canonicalName = clazz.getCanonicalName();
		return (canonicalName != null ? canonicalName : clazz.getTypeName());
	}

	/**
	 * Return the qualified name of the given class: usually simply
	 * the class name, but component type class name + "[]" for arrays.
	 * @param clazz the class
	 * @return the qualified name of the class
	 */
	public static String getQualifiedName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return clazz.getTypeName();
	}

	/**
	 * Determine whether the given class has a public constructor with the given signature.
	 * <p>Essentially translates {@code NoSuchMethodException} to "false".
	 * @param clazz the clazz to analyze
	 * @param paramTypes the parameter types of the method
	 * @return whether the class has a corresponding constructor
	 * @see Class#getConstructor
	 */
	public static boolean hasConstructor(Class<?> clazz, Class<?>... paramTypes) {
		return (getConstructorIfAvailable(clazz, paramTypes) != null);
	}

	/**
	 * Determine whether the given class has a public constructor with the given signature,
	 * and return it if available (else return {@code null}).
	 * <p>Essentially translates {@code NoSuchMethodException} to {@code null}.
	 * @param clazz the clazz to analyze
	 * @param paramTypes the parameter types of the method
	 * @return the constructor, or {@code null} if not found
	 * @see Class#getConstructor
	 */
	public static <T> Constructor<T> getConstructorIfAvailable(Class<T> clazz, Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		try {
			return clazz.getConstructor(paramTypes);
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	/**
	 * Determine whether the given class has a public method with the given signature,
	 * and return it if available (else return {@code null}).
	 * <p>In case of any signature specified, only returns the method if there is a
	 * unique candidate, i.e. a single public method with the specified name.
	 * <p>Essentially translates {@code NoSuchMethodException} to {@code null}.
	 * @param clazz the clazz to analyze
	 * @param methodName the name of the method
	 * @param paramTypes the parameter types of the method
	 * (can be {@code null} to indicate any signature)
	 * @return the method, or {@code null} if not found
	 * @see Class#getMethod
	 */
	public static Method getMethodIfAvailable(Class<?> clazz, String methodName, Class<?> ... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		if (paramTypes == null) {
			Set<Method> candidates = findMethodCandidatesByName(clazz, methodName);
			if (candidates.size() == 1) {
				return candidates.iterator().next();
			}
			return null;
		}
		try {
			return clazz.getMethod(methodName, paramTypes);
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	private static Set<Method> findMethodCandidatesByName(Class<?> clazz, String methodName) {
		Set<Method> candidates = new HashSet<>(1);
		Method[] methods = clazz.getMethods();
		for (Method method : methods) {
			if (methodName.equals(method.getName())) {
				candidates.add(method);
			}
		}
		return candidates;
	}

}
