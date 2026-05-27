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

package play.data.internal.binding.validation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Stores and exposes information about data-binding and validation errors
 * for a specific object.
 *
 * <p>Field names are typically properties of the target object (for example, "name"
 * when binding to a customer object). Implementations may also support nested
 * fields in case of nested objects (for example, "address.street").
 *
 * <p>Note: {@code Errors} objects are single-threaded.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see BindingResult
 */
public interface Errors {

	String NESTED_PATH_SEPARATOR = ".";


	/**
	 * Return the name of the bound root object.
	 */
	String getObjectName();

	/**
	 * Register a global error for the entire target object,
	 * using the given error description.
	 * @param errorCode error code, interpretable as a message key
	 * @param errorArgs error arguments, for argument binding via MessageFormat
	 * (can be {@code null})
	 * @param defaultMessage fallback default message
	 * @see #rejectValue(String, String, Object[], String)
	 */
	void reject(String errorCode, Object [] errorArgs, String defaultMessage);

	/**
	 * Register a field error for the specified field of the current object,
	 * using the given error description.
	 * <p>The field name may be {@code null} or empty String to indicate
	 * the current object itself rather than a field of it. This may result
	 * in a corresponding field error within the nested object graph or a
	 * global error if the current object is the top object.
	 * @param field the field name (may be {@code null} or empty String)
	 * @param errorCode error code, interpretable as a message key
	 * @param errorArgs error arguments, for argument binding via MessageFormat
	 * (can be {@code null})
	 * @param defaultMessage fallback default message
	 * @see #reject(String, Object[], String)
	 */
	void rejectValue(String field, String errorCode,
			Object [] errorArgs, String defaultMessage);

	/**
	 * Determine if there were any errors.
	 */
	default boolean hasErrors() {
		return (!getGlobalErrors().isEmpty() || !getFieldErrors().isEmpty());
	}

	/**
	 * Determine the total number of errors.
	 * @see #getGlobalErrorCount()
	 */
	default int getErrorCount() {
		return (getGlobalErrors().size() + getFieldErrors().size());
	}

	/**
	 * Get all errors, both global and field ones.
	 * @return a list of {@link ObjectError}/{@link FieldError} instances
	 * @see #getGlobalErrors()
	 * @see #getFieldErrors()
	 */
	default List<ObjectError> getAllErrors() {
		return Stream.concat(getGlobalErrors().stream(), getFieldErrors().stream()).toList();
	}

	/**
	 * Determine the number of global errors.
	 */
	default int getGlobalErrorCount() {
		return getGlobalErrors().size();
	}

	/**
	 * Get all global errors.
	 * @return a list of {@link ObjectError} instances
	 * @see #getFieldErrors()
	 */
	List<ObjectError> getGlobalErrors();

	/**
	 * Get all errors associated with a field.
	 * @return a List of {@link FieldError} instances
	 * @see #getGlobalErrors()
	 */
	List<FieldError> getFieldErrors();

	/**
	 * Get the first error associated with the given field, if any.
	 * @param field the field name
	 * @return the field-specific error, or {@code null}
	 */
	default FieldError getFieldError(String field) {
		return getFieldErrors().stream().filter(error -> field.equals(error.getField())).findFirst().orElse(null);
	}

	/**
	 * Return the current value of the given field, either the current
	 * bean property value or a rejected update from the last binding.
	 * <p>Allows for convenient access to user-specified field values,
	 * even if there were type mismatches.
	 * @param field the field name
	 * @return the current value of the given field
	 * @see #getFieldType(String)
	 */
	Object getFieldValue(String field);

	/**
	 * Determine the type of the given field, as far as possible.
	 * <p>Implementations should be able to determine the type even
	 * when the field value is {@code null}, for example from some
	 * associated descriptor.
	 * @param field the field name
	 * @return the type of the field, or {@code null} if not determinable
	 * @see #getFieldValue(String)
	 */
	default Class<?> getFieldType(String field) {
		return Optional.ofNullable(getFieldValue(field)).map(Object::getClass).orElse(null);
	}

	/**
	 * Return a summary of the recorded errors,
	 * for example, for inclusion in an exception message.
	 */
	@Override
	String toString();

}
