package de.whitefrog.frogr.test;

import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.repository.BaseModelRepository;

public class PersonRepository extends BaseModelRepository<Person> {
  public PersonRepository(Service service) {
    super(service);
  }
}
