/**
 * Declarative contract annotations and rendering extension points for application code.
 *
 * <p>Use this package to annotate supported program elements and customize how masked values appear in generated
 * violation messages.
 *
 * <p>Contract annotations express programming invariants rather than
 * user-input validation rules. Parameter contracts are preconditions,
 * method-level contracts on non-void methods are postconditions, field
 * annotations are reusable metadata, and annotation-type usage enables custom
 * composed contracts. For object values, built-in semantic contracts ignore
 * {@code null}; use nullness-specific tooling when null must be rejected.
 */
@NullMarked
package media.barney.contract;

import org.jspecify.annotations.NullMarked;
