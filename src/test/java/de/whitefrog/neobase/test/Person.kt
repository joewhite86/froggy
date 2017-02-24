package de.whitefrog.neobase.test

import de.whitefrog.neobase.model.Entity
import de.whitefrog.neobase.model.annotation.*
import org.neo4j.graphdb.Direction

class Person : Entity() {
  @Uuid
  @Unique
  var uniqueField: String? = null
  var field: String? = null
  @Lazy
  @RelatedTo(direction = Direction.OUTGOING, type = "Likes")
  var likes: List<Person>? = null
}
