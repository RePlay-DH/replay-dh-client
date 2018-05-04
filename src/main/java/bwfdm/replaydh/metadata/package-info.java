/**
 * Contains the abstract interface to (potentially external) metadata repositories.
 * Metadata in this context is assumed to be a collection of key-value entries that are
 * wrapped together into a record. Depending on the underlying metadata schema of a
 * repository certain limitations apply like the multiplicity of entries with the same
 * name within a single record, or the values assignable to respective names.
 * <p>
 * Unless otherwise noted all methods on {@code MetadataXX} classes in this package or
 * derived implementations can throw {@link bwfdm.replaydh.metadata.MetadataException}
 * on illegal input or when an operation fails.
 */
package bwfdm.replaydh.metadata;
