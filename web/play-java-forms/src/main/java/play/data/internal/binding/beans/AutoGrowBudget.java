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

/**
 * Tracks cumulative auto-growing performed while binding a single object graph.
 *
 * <p>The budget is intentionally mutable and shared by nested property accessors so a deeply nested
 * bind can be limited as one operation, rather than as separate per-accessor limits.
 */
public final class AutoGrowBudget {

	private final int limit;

	private int operations;

	public AutoGrowBudget(int limit) {
		if (limit < 0) {
			throw new IllegalArgumentException("Auto-grow operation limit must not be negative");
		}
		this.limit = limit;
	}

	public int getLimit() {
		return this.limit;
	}

	public int getOperations() {
		return this.operations;
	}

	public boolean canConsume(int amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("Auto-grow operation amount must not be negative");
		}
		return amount <= this.limit - this.operations;
	}

	public void consume(int amount) {
		if (!canConsume(amount)) {
			throw new IllegalStateException("Auto-grow operation limit exceeded");
		}
		this.operations += amount;
	}

}
