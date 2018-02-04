package de.whitefrog.frogr.test.model

import de.whitefrog.frogr.model.Entity
import de.whitefrog.frogr.model.annotation.*
import org.neo4j.graphdb.Direction
import java.util.*

class Person(var field: String? = null) : Entity() {
  enum class Age { Old, Mature, Child}
  @Uuid
  @Unique
  var uniqueField: String? = null
  var number: Long? = null
  var age: Age? = null
  var dateField: Date? = null
  @NullRemove
  var nullRemoveField: String? = null
  @Lazy
  @RelatedTo(direction = Direction.OUTGOING, type = "Likes")
  var likes: ArrayList<Person> = ArrayList()
  @RelatedTo(direction = Direction.BOTH, type = "MarriedWith")
  var marriedWith: Person? = null

  override fun equals(other: Any?): Boolean {
    if(other !is Person) return false
    val eq = super.equals(other)
    if(!eq && id < 0 && other.id < 0 && field != null) {
      return field == other.field
    }
    return eq
  }

  override fun toString(): String {
    val sField = if(field != null) " ($field)" else ""
    return "Person$sField"
  }
}
