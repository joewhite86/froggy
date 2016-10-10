package de.whitefrog.neobase.collection;

import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.relationship.Relationship;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.QueryField;
import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.persistence.FieldDescriptor;
import de.whitefrog.neobase.persistence.ModelCache;
import de.whitefrog.neobase.persistence.Persistence;
import de.whitefrog.neobase.persistence.Relationships;
import de.whitefrog.neobase.repository.Repository;
import de.whitefrog.neobase.Service;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.kernel.impl.core.RelationshipProxy;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class ExecutionResultIterator<T extends de.whitefrog.neobase.model.Model> extends ResultIterator<T> {
  private Service service;
  private final Result results;
  private final SearchParameter params;
  private Map<String, Object> next = null;

  @SuppressWarnings("unchecked")
  public ExecutionResultIterator(Repository<T> repository, Result results, SearchParameter params) {
    super(repository, results);
    this.params = params;
    this.results = results;
  }

  private Repository repository(Node node) {
    return service.repository((String) node.getProperty(Base.Type));
  }

  @Override
  public boolean hasNext() {
    return next != null || results.hasNext();
  }

  @Override
  @SuppressWarnings("unchecked")
  public T next() {
    Map<String, Object> result = next != null? next: results.next();
    Node node = (Node) result.get("e");
    T model = (T) (repository() != null? repository(): repository(node))
      .createModel(node, params.fieldList());
    if(result.size() > 1) {
      Map<String, List<Base>> map = new HashMap<>();
      boolean nextFound = false;
      while(!nextFound && results.hasNext()) {
        next = results.next();
        Node nextNode = (Node) next.get("e");
        if(node.equals(nextNode)) {
          for(String fieldName: next.keySet()) {
            if(fieldName.equals("e")) continue;
            PropertyContainer item = (PropertyContainer) next.get(fieldName);
            if(!map.containsKey(fieldName)) map.put(fieldName, new ArrayList<>());
            FieldList fields = params.fieldList().containsField(fieldName)?
              params.fieldList().get(fieldName).subFields(): FieldList.parseFields(Base.AllFields);
            Base base = Persistence.get(item, fields);
            map.get(fieldName).add(base);
          }
        }
        else {
          nextFound = true;
        }
      }
      for(String fieldName: map.keySet()) {
        try {
          FieldDescriptor descriptor = Persistence.cache().fieldDescriptor(model.getClass(), fieldName);
          descriptor.field().set(model, Set.class.isAssignableFrom(descriptor.field().getType())?
            new HashSet(map.get(fieldName)): map.get(fieldName));
        } catch(IllegalAccessException e) {
          e.printStackTrace();
        }
      }
      if(!nextFound) next = null;
    }
    return model;
  }

  @Override
  public void remove() {
    results.remove();
  }

  @Override
  public void close() {
    if(results != null) results.close();
  }
}
