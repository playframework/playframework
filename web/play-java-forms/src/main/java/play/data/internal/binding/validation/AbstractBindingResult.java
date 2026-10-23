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

import java.io.Serializable;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import play.data.internal.binding.util.ObjectUtils;
import play.data.internal.binding.util.StringUtils;

/**
 * Abstract implementation of the {@link BindingResult} interface and
 * its super-interface {@link Errors}. Encapsulates common management of
 * {@link ObjectError ObjectErrors} and {@link FieldError FieldErrors}.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see Errors
 */
@SuppressWarnings("serial")
public abstract class AbstractBindingResult extends AbstractErrors implements BindingResult, Serializable {

	private final String objectName;

	private MessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();

	private final List<ObjectError> errors = new ArrayList<>();


	/**
	 * Create a new AbstractBindingResult instance.
	 * @param objectName the name of the target object
	 * @see DefaultMessageCodesResolver
	 */
	protected AbstractBindingResult(String objectName) {
		this.objectName = objectName;
	}

	/**
	 * Return the strategy to use for resolving errors into message codes.
	 */
	public MessageCodesResolver getMessageCodesResolver() {
		return this.messageCodesResolver;
	}


	//---------------------------------------------------------------------
	// Implementation of the Errors interface
	//---------------------------------------------------------------------

	@Override
	public String getObjectName() {
		return this.objectName;
	}

	@Override
	public void reject(String errorCode, Object [] errorArgs, String defaultMessage) {
		addError(new ObjectError(getObjectName(), resolveMessageCodes(errorCode), errorArgs, defaultMessage));
	}

	@Override
	public void rejectValue(String field, String errorCode,
			Object [] errorArgs, String defaultMessage, Locale locale) {

		if (!StringUtils.hasLength(field)) {
			reject(errorCode, errorArgs, defaultMessage);
			return;
		}

		String fixedField = fixedField(field);
		Object newVal = getActualFieldValue(fixedField, locale);
		FieldError fe = new FieldError(getObjectName(), fixedField, newVal, false,
				resolveMessageCodes(errorCode, field, locale), errorArgs, defaultMessage);
		addError(fe);
	}

	@Override
	public boolean hasErrors() {
		return !this.errors.isEmpty();
	}

	@Override
	public int getErrorCount() {
		return this.errors.size();
	}

	@Override
	public List<ObjectError> getAllErrors() {
		return Collections.unmodifiableList(this.errors);
	}

	@Override
	public List<ObjectError> getGlobalErrors() {
		List<ObjectError> result = new ArrayList<>();
		for (ObjectError objectError : this.errors) {
			if (!(objectError instanceof FieldError)) {
				result.add(objectError);
			}
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public List<FieldError> getFieldErrors() {
		List<FieldError> result = new ArrayList<>();
		for (ObjectError objectError : this.errors) {
			if (objectError instanceof FieldError fieldError) {
				result.add(fieldError);
			}
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public FieldError getFieldError(String field) {
		String fixedField = fixedField(field);
		for (ObjectError objectError : this.errors) {
			if (objectError instanceof FieldError fieldError && isMatchingFieldError(fixedField, fieldError)) {
				return fieldError;
			}
		}
		return null;
	}

	@Override
	public Object getFieldValue(String field, Locale locale) {
		FieldError fieldError = getFieldError(field);
		// Use rejected value in case of error, current field value otherwise.
		if (fieldError != null) {
			Object value = fieldError.getRejectedValue();
			// Do not apply formatting on binding failures like type mismatches.
			return (fieldError.isBindingFailure() || getTarget() == null ? value : formatFieldValue(field, value, locale));
		}
		else if (getTarget() != null) {
			Object value = getActualFieldValue(fixedField(field), locale);
			return formatFieldValue(field, value, locale);
		}
		else {
			return null;
		}
	}

	/**
	 * This default implementation determines the type based on the actual
	 * field value, if any. Subclasses should override this to determine
	 * the type from a descriptor, even for {@code null} values.
	 * @see #getActualFieldValue
	 */
	@Override
	public Class<?> getFieldType(String field, Locale locale) {
		if (getTarget() != null) {
			Object value = getActualFieldValue(fixedField(field), locale);
			if (value != null) {
				return value.getClass();
			}
		}
		return null;
	}


	//---------------------------------------------------------------------
	// Implementation of BindingResult interface
	//---------------------------------------------------------------------

	public String[] resolveMessageCodes(String errorCode) {
		return getMessageCodesResolver().resolveMessageCodes(errorCode, getObjectName());
	}

	@Override
	public String[] resolveMessageCodes(String errorCode, String field, Locale locale) {
		return getMessageCodesResolver().resolveMessageCodes(
				errorCode, getObjectName(), fixedField(field), getFieldType(field, locale));
	}

	@Override
	public void addError(ObjectError error) {
		this.errors.add(error);
	}


	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof BindingResult that &&
				getObjectName().equals(that.getObjectName()) &&
				ObjectUtils.nullSafeEquals(getTarget(), that.getTarget()) &&
				getAllErrors().equals(that.getAllErrors())));
	}

	@Override
	public int hashCode() {
		return getObjectName().hashCode();
	}


	//---------------------------------------------------------------------
	// Template methods to be implemented/overridden by subclasses
	//---------------------------------------------------------------------

	/**
	 * Return the wrapped target object.
	 */
	@Override
	public abstract Object getTarget();

	/**
	 * Extract the actual field value for the given field.
	 * @param field the field to check
	 * @return the current value of the field
	 */
	protected abstract Object getActualFieldValue(String field, Locale locale);

	/**
	 * Format the given value for the specified field.
	 * <p>The default implementation simply returns the field value as-is.
	 * @param field the field to check
	 * @param value the value of the field (either a rejected value
	 * other than from a binding error, or an actual field value)
	 * @return the formatted value
	 */
	protected Object formatFieldValue(String field, Object value, Locale locale) {
		return value;
	}

}
