package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.cypher.QueryBuilder;
import de.whitefrog.neobase.model.Base;
import de.whitefrog.neobase.model.SaveContext;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.Filter;
import de.whitefrog.neobase.model.rest.SearchParameter;
import de.whitefrog.neobase.service.Search;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface Repository<T extends Base> {
  boolean contains(T entity);

  T createModel();

  T createModel(PropertyContainer node);

  T createModel(PropertyContainer node, FieldList fields);

  void dispose();

  T fetch(T model, String... fields);

  T fetch(T model, FieldList fields);

  T fetch(T model, boolean refetch, FieldList fields);

  boolean filter(PropertyContainer node, Collection<Filter> filters);

  /**
   * Get a node by id
   *
   * @param id PropertyContainerode id
   * @return The node if found, otherwise a Exception will be thrown
   */
  T find(long id, String... fields);

  T find(long id, List<String> fields);

  T find(long id, FieldList fields);

  Stream<T> find(String property, Object value);

  Stream<T> find(SearchParameter params);
  
  Stream<T> findIndexed(String field, Object value);

  Stream<T> findIndexed(String field, Object value, SearchParameter params);

  Stream<T> findIndexed(Index index, String field, Object value);

  Stream<T> findIndexed(Index index, String field, Object value, SearchParameter params);
  
  T findByUuid(String uuid);

  String getType();

  Class<?> getModelClass();
  
  /**
   * Get the database instance associated with the controller
   *
   * @return Database instance
   */
  GraphDatabaseService graph();

  QueryBuilder queryBuilder();

  String queryIdentifier();

  

  Index index();

  Index index(String indexPropertyContainerame);

  void index(T model, String name, Object value);

  void index(Index index, T model, String name, Object value);

  Map<String, String> indexConfig(String index);

  Index indexForField(String fieldPropertyContainerame);

  void indexRemove(Index index, PropertyContainer node);

  void indexRemove(PropertyContainer node, String field);

  void indexRemove(PropertyContainer node);

  void indexRemove(Index index, PropertyContainer node, String field);

  /**
   * Delete a model
   *
   * @param model Model to delete
   */
  void remove(T model);

  void save(T model);

  void save(T... entities);

  void save(SaveContext<T> context);

  Search search();

  Service service();

  void sort(List<T> list, List<SearchParameter.OrderBy> orderBy);
}
