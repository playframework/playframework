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

import play.data.internal.binding.beans.PropertyAccessException;
import play.data.internal.binding.context.support.DefaultMessageSourceResolvable;
import play.data.internal.binding.util.Assert;
import play.data.internal.binding.util.ObjectUtils;
import play.data.internal.binding.util.StringUtils;

/**
 * Default binding error processor.
 *
 * <p>Creates a {@code FieldError} for each {@code PropertyAccessException}
 * given, using the {@code PropertyAccessException}'s error code ("typeMismatch",
 * "methodInvocation") for resolving message codes.
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @see BeanPropertyBindingResult#addError
 * @see play.data.internal.binding.beans.PropertyAccessException#getErrorCode
 * @see play.data.internal.binding.beans.TypeMismatchException#ERROR_CODE
 * @see play.data.internal.binding.beans.MethodInvocationException#ERROR_CODE
 */
public class DefaultBindingErrorProcessor implements BindingErrorProcessor {

	@Override
	public void processPropertyAccessException(PropertyAccessException ex, BindingResult bindingResult) {
		// Create field error with the code of the exception, for example, "typeMismatch".
		String field = ex.getPropertyName();
		Assert.state(field != null, "No field in exception");
		String[] codes = bindingResult.resolveMessageCodes(ex.getErrorCode(), field);
		Object[] arguments = getArgumentsForBindError(bindingResult.getObjectName(), field);
		Object rejectedValue = ex.getValue();
		if (ObjectUtils.isArray(rejectedValue)) {
			rejectedValue = StringUtils.arrayToCommaDelimitedString(ObjectUtils.toObjectArray(rejectedValue));
		}
		bindingResult.addError(new BindingFieldError(
				bindingResult.getObjectName(), field, rejectedValue, codes, arguments, ex));
	}

	/**
	 * Return FieldError arguments for a binding error on the given field.
	 * Invoked for each type mismatch.
	 * <p>The default implementation returns a single argument indicating the field name
	 * (of type DefaultMessageSourceResolvable, with "objectName.field" and "field" as codes).
	 * @param objectName the name of the target object
	 * @param field the field that caused the binding error
	 * @return the Object array that represents the FieldError arguments
	 * @see play.data.internal.binding.validation.FieldError#getArguments
	 * @see play.data.internal.binding.context.support.DefaultMessageSourceResolvable
	 */
	protected Object[] getArgumentsForBindError(String objectName, String field) {
		String[] codes = new String[] {objectName + Errors.NESTED_PATH_SEPARATOR + field, field};
		return new Object[] {new DefaultMessageSourceResolvable(codes, field)};
	}


	/**
	 * Subclass of {@code FieldError} with default message rendering matching previous behavior.
	 */
	@SuppressWarnings("serial")
	private static class BindingFieldError extends FieldError implements Serializable {

		public BindingFieldError(String objectName, String field, Object rejectedValue, String[] codes,
				Object[] arguments, PropertyAccessException ex) {

			super(objectName, field, rejectedValue, true, codes, arguments, ex.getLocalizedMessage());
			wrap(ex);
		}

		@Override
		public boolean shouldRenderDefaultMessage() {
			return false;
		}
	}

}
