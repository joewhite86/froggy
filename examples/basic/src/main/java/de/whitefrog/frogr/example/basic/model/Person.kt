package de.whitefrog.frogr.example.basic.model

import de.whitefrog.frogr.example.basic.RelationshipTypes
import de.whitefrog.frogr.model.Entity
import de.whitefrog.frogr.model.annotation.*
import org.neo4j.graphdb.Direction
import java.util.*

class Person() : Entity() {
  constructor(name: String) : this() {
    this.name = name
  }
  
  // Unique and required property
  @Unique
  @Indexed(type = IndexType.LowerCase)
  @Required
  var name: String? = null

  // Relationship to another single model
  @RelatedTo(type = RelationshipTypes.MarriedWith, direction = Direction.BOTH)
  var marriedWith: MarriedWith? = null
  // Relationship to a collection of models
  @Lazy
  @RelatedTo(type = RelationshipTypes.ChildOf, direction = Direction.OUTGOING)
  var parents: List<Person> = ArrayList()
  @Lazy
  @RelatedTo(type = RelationshipTypes.ChildOf, direction = Direction.INCOMING)
  var children: List<Person> = ArrayList()
  
  companion object {
    @JvmField val Name = "name"
    @JvmField val MarriedWith = "marriedWith"
    @JvmField val Children = "children"
  }
}
