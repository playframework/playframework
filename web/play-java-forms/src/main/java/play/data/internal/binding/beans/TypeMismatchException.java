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

package play.data.internal.binding.beans;

import java.beans.PropertyChangeEvent;

import play.data.internal.binding.util.ClassUtils;

/**
 * Exception thrown on a type mismatch when trying to set a bean property.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class TypeMismatchException extends PropertyAccessException {

	/**
	 * Error code that a type mismatch error will be registered with.
	 */
	public static final String ERROR_CODE = "typeMismatch";


	private String propertyName;

	private final transient Object value;


	/**
	 * Create a new {@code TypeMismatchException}.
	 * @param propertyChangeEvent the PropertyChangeEvent that resulted in the problem
	 * @param requiredType the required target type (or {@code null} if not known)
	 * @param cause the root cause (may be {@code null})
	 */
	public TypeMismatchException(PropertyChangeEvent propertyChangeEvent, Class<?> requiredType,
			Throwable cause) {

		super(propertyChangeEvent,
				"Failed to convert property value of type '" +
				ClassUtils.getDescriptiveType(propertyChangeEvent.getNewValue()) + "'" +
				(requiredType != null ?
				" to required type '" + ClassUtils.getQualifiedName(requiredType) + "'" : "") +
				(propertyChangeEvent.getPropertyName() != null ?
				" for property '" + propertyChangeEvent.getPropertyName() + "'" : "") +
				(cause != null ? "; " + cause.getMessage() : ""),
				cause);
		this.propertyName = propertyChangeEvent.getPropertyName();
		this.value = propertyChangeEvent.getNewValue();
	}


	/**
	 * Return the name of the affected property, if available.
	 */
	@Override
	public String getPropertyName() {
		return this.propertyName;
	}

	/**
	 * Return the offending value (may be {@code null}).
	 */
	@Override
	public Object getValue() {
		return this.value;
	}

	@Override
	public String getErrorCode() {
		return ERROR_CODE;
	}

}
