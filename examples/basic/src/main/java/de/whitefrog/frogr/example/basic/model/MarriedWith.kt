package de.whitefrog.frogr.example.basic.model

import de.whitefrog.frogr.model.relationship.FRelationship

class MarriedWith(from: Person, to: Person): FRelationship<Person, Person>(from, to) {
  var years: Long? = null
}