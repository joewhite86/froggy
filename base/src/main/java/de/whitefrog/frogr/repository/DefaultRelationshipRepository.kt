package de.whitefrog.frogr.repository

import de.whitefrog.frogr.model.relationship.Relationship
import de.whitefrog.frogr.model.relationship.FRelationship

/**
 * Will be used by [RepositoryFactory] method when no other repository was found.
 */
class DefaultRelationshipRepository<T : Relationship<*, *>>(modelName: String) : BaseRelationshipRepository<T>(modelName) {
  override fun getModelClass(): Class<*> {
    if (modelClass == null) {
      modelClass = cache().getModel(type)
      if (modelClass == null) modelClass = FRelationship::class.java
    }

    return modelClass
  }
}
