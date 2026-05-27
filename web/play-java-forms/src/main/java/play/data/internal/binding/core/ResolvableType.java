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

package play.data.internal.binding.core;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import play.data.internal.binding.util.Assert;
import play.data.internal.binding.util.ClassUtils;
import play.data.internal.binding.util.ConcurrentReferenceHashMap;
import play.data.internal.binding.util.ObjectUtils;
import play.data.internal.binding.util.StringUtils;

/**
 * Encapsulates a Java {@link java.lang.reflect.Type}, providing access to
 * {@link #getSuperType() supertypes}, {@link #getInterfaces() interfaces}, and
 * {@link #getGeneric(int...) generic parameters} along with the ability to ultimately
 * {@link #resolve() resolve} to a {@link java.lang.Class}.
 *
 * <p>A {@code ResolvableType} may be obtained from a {@linkplain #forField(Field) field},
 * a method parameter,
 * a {@linkplain #forClass(Class) class}. Most methods on this class will themselves return
 * a {@code ResolvableType}, allowing for easy navigation. For example:
 * <pre class="code">
 * private HashMap&lt;Integer, List&lt;String&gt;&gt; myMap;
 *
 * public void example() {
 *     ResolvableType t = ResolvableType.forField(getClass().getDeclaredField("myMap"));
 *     t.getSuperType(); // AbstractMap&lt;Integer, List&lt;String&gt;&gt;
 *     t.asMap(); // Map&lt;Integer, List&lt;String&gt;&gt;
 *     t.getGeneric(0).resolve(); // Integer
 *     t.getGeneric(1).resolve(); // List
 *     t.getGeneric(1); // List&lt;String&gt;
 *     t.resolveGeneric(1, 0); // String
 * }
 * </pre>
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Yanming Zhou
 * @see #forField(Field)
 * @see #forClass(Class)
 * @see #forType(Type)
 */
public class ResolvableType {

	/**
	 * {@code ResolvableType} returned when no value is available. {@code NONE} is used
	 * in preference to {@code null} so that multiple method calls can be safely chained.
	 */
	public static final ResolvableType NONE = new ResolvableType(EmptyType.INSTANCE, null, 0);

	private static final ResolvableType[] EMPTY_TYPES_ARRAY = new ResolvableType[0];

	private static final ConcurrentReferenceHashMap<ResolvableType, ResolvableType> cache =
			new ConcurrentReferenceHashMap<>(256);


	/**
	 * The underlying Java type being managed.
	 */
	private final Type type;

	/**
	 * The {@code VariableResolver} to use or {@code null} if no resolver is available.
	 */
	private final VariableResolver variableResolver;

	private final Integer hash;

	private Class<?> resolved;

	private transient volatile ResolvableType superType;

	private transient volatile ResolvableType [] interfaces;

	private transient volatile ResolvableType [] generics;


	/**
	 * Private constructor used to create a new {@code ResolvableType} for cache key purposes,
	 * with no upfront resolution.
	 */
	private ResolvableType(
			Type type, VariableResolver variableResolver) {

		this.type = type;
		this.variableResolver = variableResolver;
		this.hash = calculateHashCode();
		this.resolved = null;
	}

	/**
	 * Private constructor used to create a new {@code ResolvableType} for uncached purposes,
	 * or cache values, with upfront resolution and a lazily calculated or pre-calculated hash.
	 */
	private ResolvableType(Type type, VariableResolver variableResolver, Integer hash) {

		this.type = type;
		this.variableResolver = variableResolver;
		this.hash = hash;
		this.resolved = resolveClass();
	}

	/**
	 * Private constructor used to create a new {@code ResolvableType} on a {@link Class} basis.
	 * <p>Avoids all {@code instanceof} checks in order to create a straight {@link Class} wrapper.
	 */
	private ResolvableType(Class<?> clazz) {
		this.resolved = (clazz != null ? clazz : Object.class);
		this.type = this.resolved;
		this.variableResolver = null;
		this.hash = null;
	}


	/**
	 * Return the underling Java {@link Type} being managed.
	 */
	public Type getType() {
		return this.type;
	}

	/**
	 * Return this type as a resolved {@code Class}, falling back to
	 * {@link java.lang.Object} if no specific class can be resolved.
	 * @return the resolved {@link Class} or the {@code Object} fallback
	 * @see #resolve(Class)
	 */
	public Class<?> toClass() {
		return resolve(Object.class);
	}

	/**
	 * Return {@code true} if this type resolves to a Class that represents an array.
	 * @see #getComponentType()
	 */
	public boolean isArray() {
		if (this == NONE) {
			return false;
		}
		return ((this.type instanceof Class<?> clazz && clazz.isArray()) ||
				this.type instanceof GenericArrayType || resolveType().isArray());
	}

	/**
	 * Return the ResolvableType representing the component type of the array or
	 * {@link #NONE} if this type does not represent an array.
	 * @see #isArray()
	 */
	public ResolvableType getComponentType() {
		if (this == NONE) {
			return NONE;
		}
		if (this.type instanceof Class<?> clazz) {
			Class<?> componentType = clazz.componentType();
			return forType(componentType, this.variableResolver);
		}
		if (this.type instanceof GenericArrayType genericArrayType) {
			return forType(genericArrayType.getGenericComponentType(), this.variableResolver);
		}
		return resolveType().getComponentType();
	}

	/**
	 * Convenience method to return this type as a resolvable {@link Collection} type.
	 * <p>Returns {@link #NONE} if this type does not implement or extend
	 * {@link Collection}.
	 * @see #as(Class)
	 * @see #asMap()
	 */
	public ResolvableType asCollection() {
		return as(Collection.class);
	}

	/**
	 * Convenience method to return this type as a resolvable {@link Map} type.
	 * <p>Returns {@link #NONE} if this type does not implement or extend
	 * {@link Map}.
	 * @see #as(Class)
	 * @see #asCollection()
	 */
	public ResolvableType asMap() {
		return as(Map.class);
	}

	/**
	 * Return this type as a {@code ResolvableType} of the specified class. Searches
	 * {@link #getSuperType() supertype} and {@link #getInterfaces() interface}
	 * hierarchies to find a match, returning {@link #NONE} if this type does not
	 * implement or extend the specified class.
	 * @param type the required type (typically narrowed)
	 * @return a {@code ResolvableType} representing this object as the specified
	 * type, or {@link #NONE} if not resolvable as that type
	 * @see #asCollection()
	 * @see #asMap()
	 * @see #getSuperType()
	 * @see #getInterfaces()
	 */
	public ResolvableType as(Class<?> type) {
		if (this == NONE) {
			return NONE;
		}
		Class<?> resolved = resolve();
		if (resolved == null || resolved == type) {
			return this;
		}
		for (ResolvableType interfaceType : getInterfaces()) {
			ResolvableType interfaceAsType = interfaceType.as(type);
			if (interfaceAsType != NONE) {
				return interfaceAsType;
			}
		}
		return getSuperType().as(type);
	}

	/**
	 * Return a {@code ResolvableType} representing the direct supertype of this type.
	 * <p>If no supertype is available this method returns {@link #NONE}.
	 * @see #getInterfaces()
	 */
	public ResolvableType getSuperType() {
		Class<?> resolved = resolve();
		if (resolved == null) {
			return NONE;
		}
		try {
			Type superclass = resolved.getGenericSuperclass();
			if (superclass == null) {
				return NONE;
			}
			ResolvableType superType = this.superType;
			if (superType == null) {
				superType = forType(superclass, this);
				this.superType = superType;
			}
			return superType;
		}
		catch (TypeNotPresentException ex) {
			// Ignore non-present types in generic signature
			return NONE;
		}
	}

	/**
	 * Return a {@code ResolvableType} array representing the direct interfaces
	 * implemented by this type. If this type does not implement any interfaces an
	 * empty array is returned.
	 * @see #getSuperType()
	 */
	public ResolvableType[] getInterfaces() {
		Class<?> resolved = resolve();
		if (resolved == null) {
			return EMPTY_TYPES_ARRAY;
		}
		ResolvableType[] interfaces = this.interfaces;
		if (interfaces == null) {
			Type[] genericIfcs = resolved.getGenericInterfaces();
			if (genericIfcs.length > 0) {
				interfaces = new ResolvableType[genericIfcs.length];
				for (int i = 0; i < genericIfcs.length; i++) {
					interfaces[i] = forType(genericIfcs[i], this);
				}
			}
			else {
				interfaces = EMPTY_TYPES_ARRAY;
			}
			this.interfaces = interfaces;
		}
		return interfaces;
	}

	/**
	 * Return {@code true} if this type contains generic parameters.
	 * @see #getGeneric(int...)
	 * @see #getGenerics()
	 */
	public boolean hasGenerics() {
		return (getGenerics().length > 0);
	}

	/**
	 * Return {@code true} if this type contains at least a generic type
	 * that is resolved. In other words, this returns {@code false} if
	 * the type contains unresolvable generics only, that is, no substitute
	 * for any of its declared type variables.
	 */
	public boolean hasResolvableGenerics() {
		if (this == NONE) {
			return false;
		}
		ResolvableType[] generics = getGenerics();
		for (ResolvableType generic : generics) {
			if (!generic.isUnresolvableTypeVariable() && !generic.isWildcardWithoutBounds()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether the underlying type is a type variable that
	 * cannot be resolved through the associated variable resolver.
	 */
	private boolean isUnresolvableTypeVariable() {
		if (this.type instanceof TypeVariable<?> variable) {
			if (this.variableResolver == null) {
				return true;
			}
			ResolvableType resolved = this.variableResolver.resolveVariable(variable);
			if (resolved == null || resolved.isUnresolvableTypeVariable() || resolved.isWildcardWithoutBounds()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether the underlying type represents a wildcard
	 * without specific bounds (i.e., equal to {@code ? extends Object}).
	 */
	private boolean isWildcardWithoutBounds() {
		if (this.type instanceof WildcardType wildcardType) {
			if (wildcardType.getLowerBounds().length == 0) {
				Type[] upperBounds = wildcardType.getUpperBounds();
				if (upperBounds.length == 0 || (upperBounds.length == 1 && Object.class == upperBounds[0])) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return a {@code ResolvableType} for the specified nesting level.
	 * @param nestingLevel the nesting level
	 * @return the {@code ResolvableType} type, or {@code #NONE}
	 */
	public ResolvableType getNested(int nestingLevel) {
		ResolvableType result = this;
		for (int i = 2; i <= nestingLevel; i++) {
			if (result.isArray()) {
				result = result.getComponentType();
			}
			else {
				// Handle derived types
				while (result != ResolvableType.NONE && !result.hasGenerics()) {
					result = result.getSuperType();
				}
				Integer index = result.getGenerics().length - 1;
				result = result.getGeneric(index);
			}
		}
		return result;
	}

	/**
	 * Return a {@code ResolvableType} representing the generic parameter for the
	 * given indexes. Indexes are zero based; for example given the type
	 * {@code Map<Integer, List<String>>}, {@code getGeneric(0)} will access the
	 * {@code Integer}. Nested generics can be accessed by specifying multiple indexes;
	 * for example {@code getGeneric(1, 0)} will access the {@code String} from the
	 * nested {@code List}. For convenience, if no indexes are specified the first
	 * generic is returned.
	 * <p>If no generic is available at the specified indexes {@link #NONE} is returned.
	 * @param indexes the indexes that refer to the generic parameter
	 * (can be omitted to return the first generic)
	 * @return a {@code ResolvableType} for the specified generic, or {@link #NONE}
	 * @see #hasGenerics()
	 * @see #getGenerics()
	 * @see #resolveGeneric(int...)
	 */
	public ResolvableType getGeneric(int ... indexes) {
		ResolvableType[] generics = getGenerics();
		if (indexes == null || indexes.length == 0) {
			return (generics.length == 0 ? NONE : generics[0]);
		}
		ResolvableType generic = this;
		for (int index : indexes) {
			generics = generic.getGenerics();
			if (index < 0 || index >= generics.length) {
				return NONE;
			}
			generic = generics[index];
		}
		return generic;
	}

	/**
	 * Return an array of {@code ResolvableType ResolvableTypes} representing the generic parameters of
	 * this type. If no generics are available an empty array is returned. If you need to
	 * access a specific generic consider using the {@link #getGeneric(int...)} method as
	 * it allows access to nested generics and protects against
	 * {@code IndexOutOfBoundsExceptions}.
	 * @return an array of {@code ResolvableType ResolvableTypes} representing the generic parameters
	 * (never {@code null})
	 * @see #hasGenerics()
	 * @see #getGeneric(int...)
	 * @see #resolveGeneric(int...)
	 */
	public ResolvableType[] getGenerics() {
		if (this == NONE) {
			return EMPTY_TYPES_ARRAY;
		}
		ResolvableType[] generics = this.generics;
		if (generics == null) {
			if (this.type instanceof Class<?> clazz) {
				Type[] typeParams = clazz.getTypeParameters();
				if (typeParams.length > 0) {
					generics = new ResolvableType[typeParams.length];
					for (int i = 0; i < generics.length; i++) {
						generics[i] = ResolvableType.forType(typeParams[i], this);
					}
				}
				else {
					generics = EMPTY_TYPES_ARRAY;
				}
			}
			else if (this.type instanceof ParameterizedType parameterizedType) {
				Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
				if (actualTypeArguments.length > 0) {
					generics = new ResolvableType[actualTypeArguments.length];
					for (int i = 0; i < actualTypeArguments.length; i++) {
						generics[i] = forType(actualTypeArguments[i], this.variableResolver);
					}
				}
				else {
					generics = EMPTY_TYPES_ARRAY;
				}
			}
			else {
				generics = resolveType().getGenerics();
			}
			this.generics = generics;
		}
		return generics;
	}

	/**
	 * Convenience method that will {@link #getGenerics() get} and {@link #resolve()
	 * resolve} generic parameters, using the specified {@code fallback} if any type
	 * cannot be resolved.
	 * @param fallback the fallback class to use if resolution fails
	 * @return an array of resolved generic parameters
	 * @see #getGenerics()
	 * @see #resolve()
	 */
	public Class<?>[] resolveGenerics(Class<?> fallback) {
		ResolvableType[] generics = getGenerics();
		Class<?>[] resolvedGenerics = new Class<?>[generics.length];
		for (int i = 0; i < generics.length; i++) {
			resolvedGenerics[i] = generics[i].resolve(fallback);
		}
		return resolvedGenerics;
	}

	/**
	 * Convenience method that will {@link #getGeneric(int...) get} and
	 * {@link #resolve() resolve} a specific generic parameter.
	 * @param indexes the indexes that refer to the generic parameter
	 * (can be omitted to return the first generic)
	 * @return a resolved {@link Class} or {@code null}
	 * @see #getGeneric(int...)
	 * @see #resolve()
	 */
	public Class<?> resolveGeneric(int... indexes) {
		return getGeneric(indexes).resolve();
	}

	/**
	 * Resolve this type to a {@link java.lang.Class}, returning {@code null}
	 * if the type cannot be resolved. This method will consider bounds of
	 * {@link TypeVariable TypeVariables} and {@link WildcardType WildcardTypes} if
	 * direct resolution fails; however, bounds of {@code Object.class} will be ignored.
	 * <p>If this method returns a non-null {@code Class} and {@link #hasGenerics()}
	 * returns {@code false}, the given type effectively wraps a plain {@code Class},
	 * allowing for plain {@code Class} processing if desirable.
	 * @return the resolved {@link Class}, or {@code null} if not resolvable
	 * @see #resolve(Class)
	 * @see #resolveGeneric(int...)
	 */
	public Class<?> resolve() {
		return this.resolved;
	}

	/**
	 * Resolve this type to a {@link java.lang.Class}, returning the specified
	 * {@code fallback} if the type cannot be resolved. This method will consider bounds
	 * of {@link TypeVariable TypeVariables} and {@link WildcardType WildcardTypes} if
	 * direct resolution fails; however, bounds of {@code Object.class} will be ignored.
	 * @param fallback the fallback class to use if resolution fails
	 * @return the resolved {@link Class} or the {@code fallback}
	 * @see #resolve()
	 * @see #resolveGeneric(int...)
	 */
	public Class<?> resolve(Class<?> fallback) {
		return (this.resolved != null ? this.resolved : fallback);
	}

	private Class<?> resolveClass() {
		if (this.type == EmptyType.INSTANCE) {
			return null;
		}
		if (this.type instanceof Class<?> clazz) {
			return clazz;
		}
		if (this.type instanceof GenericArrayType) {
			Class<?> resolvedComponent = getComponentType().resolve();
			return (resolvedComponent != null ? Array.newInstance(resolvedComponent, 0).getClass() : null);
		}
		return resolveType().resolve();
	}

	/**
	 * Resolve this type by a single level, returning the resolved value or {@link #NONE}.
	 */
	ResolvableType resolveType() {
		if (this.type instanceof ParameterizedType parameterizedType) {
			return forType(parameterizedType.getRawType(), this.variableResolver);
		}
		if (this.type instanceof WildcardType wildcardType) {
			Type resolved = resolveBounds(wildcardType.getUpperBounds());
			if (resolved == null) {
				resolved = resolveBounds(wildcardType.getLowerBounds());
			}
			return forType(resolved, this.variableResolver);
		}
		if (this.type instanceof TypeVariable<?> variable) {
			// Try default variable resolution
			if (this.variableResolver != null) {
				ResolvableType resolved = this.variableResolver.resolveVariable(variable);
				if (resolved != null) {
					return resolved;
				}
			}
			// Fallback to bounds
			return forType(resolveBounds(variable.getBounds()), this.variableResolver);
		}
		return NONE;
	}

	private ResolvableType resolveVariable(TypeVariable<?> variable) {
		if (this.type instanceof TypeVariable) {
			return resolveType().resolveVariable(variable);
		}
		if (this.type instanceof ParameterizedType parameterizedType) {
			Class<?> resolved = resolve();
			if (resolved == null) {
				return null;
			}
			TypeVariable<?>[] variables = resolved.getTypeParameters();
			Type[] typeArguments = parameterizedType.getActualTypeArguments();
			for (int i = 0; i < variables.length; i++) {
				if (ObjectUtils.nullSafeEquals(variables[i].getName(), variable.getName())) {
					return forType(typeArguments[i], this.variableResolver);
				}
			}
			Type ownerType = parameterizedType.getOwnerType();
			if (ownerType != null) {
				return forType(ownerType, this.variableResolver).resolveVariable(variable);
			}
		}
		if (this.type instanceof WildcardType) {
			ResolvableType resolved = resolveType().resolveVariable(variable);
			if (resolved != null) {
				return resolved;
			}
		}
		if (this.variableResolver != null) {
			return this.variableResolver.resolveVariable(variable);
		}
		return null;
	}


	/**
	 * Check for full equality of all type resolution artifacts:
	 * type as well as {@code VariableResolver}.
	 * @see #equalsType(ResolvableType)
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		ResolvableType otherType = (ResolvableType) other;

		if (!equalsType(otherType)) {
			return false;
		}
		if (this.variableResolver != otherType.variableResolver &&
				(this.variableResolver == null || otherType.variableResolver == null ||
				!ObjectUtils.nullSafeEquals(this.variableResolver.getSource(), otherType.variableResolver.getSource()))) {
			return false;
		}
		return true;
	}

	/**
	 * Check for type-level equality with another {@code ResolvableType}.
	 * <p>In contrast to {@link #equals(Object)},
	 * this works between different sources as well, for example, method parameters and return types.
	 * @param otherType the {@code ResolvableType} to match against
	 * @return whether the declared type and type variables match
	 */
	public boolean equalsType(ResolvableType otherType) {
		return ObjectUtils.nullSafeEquals(this.type, otherType.type);
	}

	@Override
	public int hashCode() {
		return (this.hash != null ? this.hash : calculateHashCode());
	}

	private int calculateHashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(this.type);
		if (this.variableResolver != null) {
			hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.variableResolver.getSource());
		}
		return hashCode;
	}

	/**
	 * Adapts this {@code ResolvableType} to a {@link VariableResolver}.
	 */
	VariableResolver asVariableResolver() {
		if (this == NONE) {
			return null;
		}
		return new DefaultVariableResolver(this);
	}

	/**
	 * Return a String representation of this type in its fully resolved form
	 * (including any generic parameters).
	 */
	@Override
	public String toString() {
		if (isArray()) {
			return getComponentType() + "[]";
		}
		if (this.resolved == null) {
			return "?";
		}
		if (this.type instanceof TypeVariable<?> variable) {
			if (this.variableResolver == null || this.variableResolver.resolveVariable(variable) == null) {
				// Don't bother with variable boundaries for toString()...
				// Can cause infinite recursions in case of self-references
				return "?";
			}
		}
		if (hasGenerics()) {
			return this.resolved.getName() + '<' + StringUtils.arrayToDelimitedString(getGenerics(), ", ") + '>';
		}
		return this.resolved.getName();
	}


	// Factory methods

	/**
	 * Return a {@code ResolvableType} for the specified {@link Class},
	 * using the full generic type information for assignability checks.
	 * <p>For example: {@code ResolvableType.forClass(MyArrayList.class)}.
	 * @param clazz the class to introspect ({@code null} is semantically
	 * equivalent to {@code Object.class} for typical use cases here)
	 * @return a {@code ResolvableType} for the specified class
	 */
	public static ResolvableType forClass(Class<?> clazz) {
		return new ResolvableType(clazz);
	}

	/**
	 * Return a {@code ResolvableType} for the specified {@link Field}.
	 * @param field the source field
	 * @return a {@code ResolvableType} for the specified field
	 */
	public static ResolvableType forField(Field field) {
		Assert.notNull(field, "Field must not be null");
		return forType(field.getGenericType(), (VariableResolver) null);
	}

	/**
	 * Return a {@code ResolvableType} for the specified {@link Method} return type.
	 * <p>Use this variant when the class that declares the method includes generic
	 * parameter variables that are satisfied by the implementation class.
	 * @param method the source for the method return type
	 * @param implementationClass the implementation class
	 * @return a {@code ResolvableType} for the specified method return
	 */
	public static ResolvableType forMethodReturnType(Method method, Class<?> implementationClass) {
		MethodParameter methodParameter = new MethodParameter(method, -1, implementationClass);
		return forMethodParameter(methodParameter);
	}

	/**
	 * Return a {@code ResolvableType} for the specified {@link Method} parameter with a
	 * given implementation. Use this variant when the class that declares the method
	 * includes generic parameter variables that are satisfied by the implementation class.
	 * @param method the source method (must not be {@code null})
	 * @param parameterIndex the parameter index
	 * @param implementationClass the implementation class
	 * @return a {@code ResolvableType} for the specified method parameter
	 * @see #forMethodParameter(Method, int, Class)
	 * @see #forMethodParameter(MethodParameter)
	 */
	public static ResolvableType forMethodParameter(Method method, int parameterIndex, Class<?> implementationClass) {
		MethodParameter methodParameter = new MethodParameter(method, parameterIndex, implementationClass);
		return forMethodParameter(methodParameter);
	}

	/**
	 * Return a {@code ResolvableType} for the specified {@link MethodParameter}.
	 * @param methodParameter the source method parameter (must not be {@code null})
	 * @return a {@code ResolvableType} for the specified method parameter
	 */
	public static ResolvableType forMethodParameter(MethodParameter methodParameter) {
		return forMethodParameter(methodParameter, (Type) null);
	}

	/**
	 * Return a {@code ResolvableType} for the specified {@link MethodParameter},
	 * overriding the target type to resolve with a specific given type.
	 * @param methodParameter the source method parameter (must not be {@code null})
	 * @param targetType the type to resolve (a part of the method parameter's type)
	 * @return a {@code ResolvableType} for the specified method parameter
	 */
	public static ResolvableType forMethodParameter(MethodParameter methodParameter, Type targetType) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		return forMethodParameter(methodParameter, targetType, methodParameter.getNestingLevel());
	}

	/**
	 * Return a {@code ResolvableType} for the specified {@link MethodParameter} at
	 * a specific nesting level, overriding the target type to resolve with a specific
	 * given type.
	 * @param methodParameter the source method parameter (must not be {@code null})
	 * @param targetType the type to resolve (a part of the method parameter's type)
	 * @param nestingLevel the nesting level to use
	 * @return a {@code ResolvableType} for the specified method parameter
	 */
	static ResolvableType forMethodParameter(
			MethodParameter methodParameter, Type targetType, int nestingLevel) {

		ResolvableType owner = forType(methodParameter.getContainingClass()).as(methodParameter.getDeclaringClass());
		return forType((targetType != null ? targetType : methodParameter.getGenericParameterType()),
				owner.asVariableResolver()).
				getNested(nestingLevel);
	}

	private static Type resolveBounds(Type[] bounds) {
		if (bounds.length == 0 || bounds[0] == Object.class) {
			return null;
		}
		return bounds[0];
	}

	/**
	 * Return a {@code ResolvableType} for the specified {@link Type}.
	 * @param type the source type (potentially {@code null})
	 * @return a {@code ResolvableType} for the specified {@link Type}
	 * @see #forType(Type, ResolvableType)
	 */
	public static ResolvableType forType(Type type) {
		return forType(type, (VariableResolver) null);
	}

	/**
	 * Return a {@code ResolvableType} for the specified {@link Type} backed by the given
	 * owner type.
	 * @param type the source type or {@code null}
	 * @param owner the owner type used to resolve variables
	 * @return a {@code ResolvableType} for the specified {@link Type} and owner
	 * @see #forType(Type)
	 */
	public static ResolvableType forType(Type type, ResolvableType owner) {
		VariableResolver variableResolver = null;
		if (owner != null) {
			variableResolver = owner.asVariableResolver();
		}
		return forType(type, variableResolver);
	}

	/**
	 * Return a {@code ResolvableType} for the specified {@link Type} backed by a given
	 * {@link VariableResolver}.
	 * @param type the source type or {@code null}
	 * @param variableResolver the variable resolver or {@code null}
	 * @return a {@code ResolvableType} for the specified {@link Type} and {@link VariableResolver}
	 */
	static ResolvableType forType(Type type, VariableResolver variableResolver) {
		if (type == null) {
			return NONE;
		}

		// For simple Class references, build the wrapper right away -
		// no expensive resolution necessary, so not worth caching...
		if (type instanceof Class) {
			return new ResolvableType(type, variableResolver, null);
		}

		// Purge empty entries on access since we don't have a clean-up thread or the like.
		cache.purgeUnreferencedEntries();

		// Check the cache - we may have a ResolvableType which has been resolved before...
		ResolvableType resultType = new ResolvableType(type, variableResolver);
		ResolvableType cachedType = cache.get(resultType);
		if (cachedType == null) {
			cachedType = new ResolvableType(type, variableResolver, resultType.hash);
			cache.put(cachedType, cachedType);
		}
		resultType.resolved = cachedType.resolved;
		return resultType;
	}

	/**
	 * Strategy interface used to resolve {@link TypeVariable TypeVariables}.
	 */
	interface VariableResolver {

		/**
		 * Return the source of the resolver (used for hashCode and equals).
		 */
		Object getSource();

		/**
		 * Resolve the specified variable.
		 * @param variable the variable to resolve
		 * @return the resolved variable, or {@code null} if not found
		 */
		ResolvableType resolveVariable(TypeVariable<?> variable);
	}


	private static class DefaultVariableResolver implements VariableResolver {

		private final ResolvableType source;

		DefaultVariableResolver(ResolvableType resolvableType) {
			this.source = resolvableType;
		}

		@Override
		public ResolvableType resolveVariable(TypeVariable<?> variable) {
			return this.source.resolveVariable(variable);
		}

		@Override
		public Object getSource() {
			return this.source;
		}
	}

	/**
	 * Internal {@link Type} used to represent an empty value.
	 */
	static class EmptyType implements Type {

		static final Type INSTANCE = new EmptyType();
	}

}
