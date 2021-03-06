package de.whitefrog.frogr.repository

import de.whitefrog.frogr.model.relationship.BaseRelationship
import de.whitefrog.frogr.model.relationship.Relationship

/**
 * Will be used by [RepositoryFactory] method when no other repository was found.
 */
class DefaultRelationshipRepository<T : Relationship<*, *>>(modelName: String) : BaseRelationshipRepository<T>(modelName) {
  override fun getModelClass(): Class<*> {
    if (modelClass == null) {
      modelClass = cache().getModel(type)
      if (modelClass == null) modelClass = BaseRelationship::class.java
    }

    return modelClass
  }
}
