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

/**
 * General interface that represents binding results. Extends the
 * {@link Errors} interface for error registration capabilities
 * and adds binding-specific analysis and model building.
 *
 * <p>Serves as result holder for a {@link DataBinder}, obtained via
 * the {@link DataBinder#getBindingResult()} method. BindingResult
 * implementations can also be used directly.
 *
 * @author Juergen Hoeller
 * @see DataBinder
 * @see Errors
 * @see BeanPropertyBindingResult
 * @see DirectFieldBindingResult
 */
public interface BindingResult extends Errors {

	/**
	 * Return the wrapped target object, which may be a bean, an object with
	 * public fields, a Map - depending on the concrete binding strategy.
	 */
	Object getTarget();

	/**
	 * Resolve the given error code into message codes for the given field.
	 * @param errorCode the error code to resolve into message codes
	 * @param field the field to resolve message codes for
	 * @return the resolved message codes
	 */
	String[] resolveMessageCodes(String errorCode, String field);

	/**
	 * Add a custom {@link ObjectError} or {@link FieldError} to the errors list.
	 * <p>Intended to be used by cooperating binding error processors.
	 * @see ObjectError
	 * @see FieldError
	 */
	void addError(ObjectError error);

}
