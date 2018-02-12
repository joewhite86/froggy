package de.whitefrog.frogr.model

/**
 * Base interface for all model entities.
 */
interface Model : Base {

  var model: String?

  companion object {
    const val Model = "model"
  }
}
