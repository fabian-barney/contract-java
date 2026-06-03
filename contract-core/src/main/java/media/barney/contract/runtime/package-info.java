/**
 * Runtime bridge invoked by annotation-processor-generated contract checks.
 *
 * <p>Types in this package are part of the supported generated-code surface; applications normally use
 * {@code media.barney.contract} annotations directly.
 *
 * <p>Generated checks call this package to evaluate built-in semantic
 * contracts, render masked values, and throw the documented JDK exception
 * types. Parameter and constructor-parameter precondition failures throw
 * {@link java.lang.IllegalArgumentException}; return-value postcondition
 * failures throw {@link java.lang.IllegalStateException}. Exact generated
 * bytecode shape and packages named {@code internal} are not public API.
 */
@NullMarked
package media.barney.contract.runtime;

import org.jspecify.annotations.NullMarked;
