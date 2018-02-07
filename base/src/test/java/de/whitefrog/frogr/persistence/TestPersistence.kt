package de.whitefrog.frogr.persistence

import de.whitefrog.frogr.Service
import de.whitefrog.frogr.exception.MissingRequiredException
import de.whitefrog.frogr.model.SaveContext
import de.whitefrog.frogr.repository.ModelRepository
import de.whitefrog.frogr.repository.RelationshipRepository
import de.whitefrog.frogr.test.TemporaryService
import de.whitefrog.frogr.test.model.Likes
import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.model.PersonRequiredField
import de.whitefrog.frogr.test.repository.PersonRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

class TestPersistence {
  private lateinit var service: Service
  private lateinit var persons: PersonRepository
  private lateinit var likesRepository: RelationshipRepository<Likes>
  private lateinit var persistence: Persistence

  @Before
  fun before() {
    service = TemporaryService()
    service.connect()
    persistence = service.persistence()
    persons = service.repository(Person::class.java)
    likesRepository = service.repository(Likes::class.java)
  }
  @After
  fun after() {
    service.shutdown()
  }
  
  @Test
  fun getNodeByUuid() {
    service.beginTx().use {
      val person = Person()
      persons.save(person)
      val node = persistence.getNode(person)
      
      val same = Person()
      same.uuid = person.uuid
      same.type = person.type
      val sameNode = persistence.getNode(same)
      
      assertThat(node).isEqualTo(sameNode)
    }
  }
  
  @Test(expected = UnsupportedOperationException::class)
  fun getNodeWithoutIdAndUuid() {
    service.beginTx().use {
      val person = Person()
      person.type = "Person"
      persistence.getNode(person)
    }
  }

  @Test(expected = MissingRequiredException::class)
  fun missingRequiredField() {
    service.beginTx().use {
      val repository = service.repository<ModelRepository<PersonRequiredField>, PersonRequiredField>(PersonRequiredField::class.java)
      val model = repository.createModel()
      persistence.save(repository, SaveContext(repository, model))
    }
  }
  
  @Test
  fun saveEnumValue() {
    service.beginTx().use {
      val person = Person()
      person.age = Person.Age.Old
      persons.save(person)
    }
  }

  @Test
  fun fetchEnumValue() {
    service.beginTx().use {
      var person = Person()
      person.age = Person.Age.Old
      persons.save(person)
      person = persons.find(person.id)
      persistence.fetch(person, "age")
      assertThat(person.age).isEqualTo(Person.Age.Old)
    }
  }
  
  @Test
  fun saveDateValue() {
    service.beginTx().use {
      val person = Person()
      person.dateField = Date()
      persistence.save(persons, SaveContext(persons, person))
    }
  }
  
  @Test(expected = UnsupportedOperationException::class)
  fun getNodeWithoutIdAndType() {
    service.beginTx().use {
      val person = Person()
      person.uuid = "123uuid"
      persistence.getNode(person)
    }
  }
}