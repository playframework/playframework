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

import play.data.internal.binding.util.StringUtils;

/**
 * Abstract implementation of the {@link Errors} interface.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 */
public abstract class AbstractErrors implements Errors {

	protected String fixedField(String field) {
		if (StringUtils.hasLength(field)) {
			return canonicalFieldName(field);
		}
		else {
			return "";
		}
	}

	protected String canonicalFieldName(String field) {
		return field;
	}

	protected boolean isMatchingFieldError(String field, FieldError fieldError) {
		if (field.equals(fieldError.getField())) {
			return true;
		}
		// Optimization: use charAt and regionMatches instead of endsWith and startsWith (SPR-11304)
		int endIndex = field.length() - 1;
		return (endIndex >= 0 && field.charAt(endIndex) == '*' &&
				(endIndex == 0 || field.regionMatches(0, fieldError.getField(), 0, endIndex)));
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName());
		sb.append(": ").append(getErrorCount()).append(" errors");
		for (ObjectError error : getAllErrors()) {
			sb.append('\n').append(error);
		}
		return sb.toString();
	}

}
