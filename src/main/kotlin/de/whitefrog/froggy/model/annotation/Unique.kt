package de.whitefrog.froggy.model.annotation

/**
 * Indicates a property must be and will be unique.
 */
@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Unique
