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

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import play.data.internal.binding.util.Assert;

/**
 * Helper class that encapsulates the specification of a method parameter: a
 * {@link Method} plus a parameter index and a nested type
 * index for a declared generic type. Useful as a specification object to pass along.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Andy Clement
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Phillip Webb
 */
public class MethodParameter {

	private final Executable executable;

	private final int parameterIndex;

	private int nestingLevel;

	/** The containing class. Could also be supplied by overriding {@link #getContainingClass()} */
	private volatile Class<?> containingClass;

	private volatile Class<?> parameterType;

	private volatile Type genericParameterType;


	/**
	 * Create a new {@code MethodParameter} for the given method, with nesting level 1.
	 * @param method the Method to specify a parameter for
	 * @param parameterIndex the index of the parameter: -1 for the method
	 * return type; 0 for the first method parameter; 1 for the second method
	 * parameter, etc.
	 */
	public MethodParameter(Method method, int parameterIndex) {
		this(method, parameterIndex, 1);
	}

	/**
	 * Create a new {@code MethodParameter} for the given method.
	 * @param method the Method to specify a parameter for
	 * @param parameterIndex the index of the parameter: -1 for the method
	 * return type; 0 for the first method parameter; 1 for the second method
	 * parameter, etc.
	 * @param nestingLevel the nesting level of the target type
	 * (1 for the top-level type; in case of a List of Lists, 2 would indicate
	 * the nested List, whereas 3 would indicate the element of the nested List)
	 */
	public MethodParameter(Method method, int parameterIndex, int nestingLevel) {
		Assert.notNull(method, "Method must not be null");
		this.executable = method;
		this.parameterIndex = validateIndex(method, parameterIndex);
		this.nestingLevel = nestingLevel;
	}

	/**
	 * Internal constructor used to create a {@code MethodParameter} with a
	 * containing class already set.
	 * @param executable the Executable to specify a parameter for
	 * @param parameterIndex the index of the parameter
	 * @param containingClass the containing class
	 */
	MethodParameter(Executable executable, int parameterIndex, Class<?> containingClass) {
		Assert.notNull(executable, "Executable must not be null");
		this.executable = executable;
		this.parameterIndex = validateIndex(executable, parameterIndex);
		this.nestingLevel = 1;
		this.containingClass = containingClass;
	}

	/**
	 * Copy constructor, resulting in an independent {@code MethodParameter} object
	 * based on the same metadata and cache state that the original object was in.
	 * @param original the original {@code MethodParameter} object to copy from
	 */
	public MethodParameter(MethodParameter original) {
		Assert.notNull(original, "Original must not be null");
		this.executable = original.executable;
		this.parameterIndex = original.parameterIndex;
		this.nestingLevel = original.nestingLevel;
		this.containingClass = original.containingClass;
		this.parameterType = original.parameterType;
		this.genericParameterType = original.genericParameterType;
	}


	/**
	 * Return the wrapped Method, if any.
	 * <p>Note: Either Method or Constructor is available.
	 * @return the Method, or {@code null} if none
	 */
	public Method getMethod() {
		return (this.executable instanceof Method method ? method : null);
	}

	/**
	 * Return the class that declares the underlying Method or Constructor.
	 */
	public Class<?> getDeclaringClass() {
		return this.executable.getDeclaringClass();
	}

	/**
	 * Return the nesting level of the target type
	 * (1 for the top-level type; in case of a List of Lists, 2 would indicate
	 * the nested List, whereas 3 would indicate the element of the nested List).
	 */
	public int getNestingLevel() {
		return this.nestingLevel;
	}

	/**
	 * Return a variant of this {@code MethodParameter} which refers to the
	 * given containing class.
	 * @param containingClass a specific containing class (potentially a
	 * subclass of the declaring class, for example, substituting a type variable)
	 * @see #getParameterType()
	 */
	public MethodParameter withContainingClass(Class<?> containingClass) {
		MethodParameter result = clone();
		result.containingClass = containingClass;
		result.parameterType = null;
		return result;
	}

	/**
	 * Return the containing class for this method parameter.
	 * @return a specific containing class (potentially a subclass of the
	 * declaring class), or otherwise simply the declaring class itself
	 * @see #getDeclaringClass()
	 */
	public Class<?> getContainingClass() {
		Class<?> containingClass = this.containingClass;
		return (containingClass != null ? containingClass : getDeclaringClass());
	}

	/**
	 * Return the type of the method/constructor parameter.
	 * @return the parameter type (never {@code null})
	 */
	public Class<?> getParameterType() {
		Class<?> paramType = this.parameterType;
		if (paramType != null) {
			return paramType;
		}
		if (getContainingClass() != getDeclaringClass()) {
			paramType = ResolvableType.forMethodParameter(this, null, 1).resolve();
		}
		if (paramType == null) {
			paramType = computeParameterType();
		}
		this.parameterType = paramType;
		return paramType;
	}

	/**
	 * Return the generic type of the method/constructor parameter.
	 * @return the parameter type (never {@code null})
	 */
	public Type getGenericParameterType() {
		Type paramType = this.genericParameterType;
		if (paramType == null) {
			if (this.parameterIndex < 0) {
				Method method = getMethod();
				paramType = (method != null ? method.getGenericReturnType() : void.class);
			}
			else {
				Type[] genericParameterTypes = this.executable.getGenericParameterTypes();
				int index = this.parameterIndex;
				paramType = (index >= 0 && index < genericParameterTypes.length ?
						genericParameterTypes[index] : computeParameterType());
			}
			this.genericParameterType = paramType;
		}
		return paramType;
	}

	private Class<?> computeParameterType() {
		if (this.parameterIndex < 0) {
			Method method = getMethod();
			if (method == null) {
				return void.class;
			}
			return method.getReturnType();
		}
		return this.executable.getParameterTypes()[this.parameterIndex];
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof MethodParameter that &&
				getContainingClass() == that.getContainingClass() &&
				this.nestingLevel == that.nestingLevel &&
				this.parameterIndex == that.parameterIndex &&
				this.executable.equals(that.executable)));
	}

	@Override
	public int hashCode() {
		return (31 * this.executable.hashCode() + this.parameterIndex);
	}

	@Override
	public String toString() {
		Method method = getMethod();
		return (method != null ? "method '" + method.getName() + "'" : "constructor") +
				" parameter " + this.parameterIndex;
	}

	@Override
	public MethodParameter clone() {
		return new MethodParameter(this);
	}


	private static int validateIndex(Executable executable, int parameterIndex) {
		int count = executable.getParameterCount();
		Assert.isTrue(parameterIndex >= -1 && parameterIndex < count,
				() -> "Parameter index needs to be between -1 and " + (count - 1));
		return parameterIndex;
	}

}
