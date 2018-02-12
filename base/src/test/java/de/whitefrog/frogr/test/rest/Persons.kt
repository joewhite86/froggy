package de.whitefrog.frogr.test.rest

import de.whitefrog.frogr.rest.service.CRUDService
import de.whitefrog.frogr.test.model.Person
import de.whitefrog.frogr.test.repository.PersonRepository
import javax.ws.rs.Path

@Path("person")
class Persons: CRUDService<PersonRepository, Person>()