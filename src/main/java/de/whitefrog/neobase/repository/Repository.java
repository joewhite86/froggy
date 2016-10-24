package de.whitefrog.neobase.repository;

import de.whitefrog.neobase.Service;
import de.whitefrog.neobase.cypher.QueryBuilder;
import de.whitefrog.neobase.model.Model;
import de.whitefrog.neobase.model.SaveContext;
import de.whitefrog.neobase.model.relationship.Relationship;
import de.whitefrog.neobase.model.rest.FieldList;
import de.whitefrog.neobase.model.rest.SearchParameter;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public interface Repository<T extends de.whitefrog.neobase.model.Model> {
  boolean contains(T entity);

  T createModel(Node node);

  T createModel(Node node, FieldList fields);

  void dispose();

  T fetch(T tag);

  T fetch(T tag, String... fields);

  T fetch(T tag, boolean refetch, String... fields);

  T fetch(T tag, FieldList fields);

  T fetch(T tag, boolean refetch, FieldList fields);

  void fetch(Relationship relationship);

  void fetch(Relationship relationship, String... fields);

  void fetch(Relationship relationship, FieldList fields);

  boolean filter(Node node, Collection<SearchParameter.PropertyFilter> filters);

  /**
   * Get a node by id
   *
   * @param id Node id
   * @return The node if found, otherwise a Exception will be thrown
   */
  T find(long id, String... fields);

  T find(long id, List<String> fields);

  Stream<T> find(String property, Object value);

  Stream<T> find(SearchParameter params);

  T findByUuid(String uuid);

  T findSingle(String property, Object value);

  Stream<T> findIndexed(String field, Object value);

  Stream<T> findIndexed(String field, Object value, SearchParameter params);

  Stream<T> findIndexed(Index<Node> index, String field, Object value);

  Stream<T> findIndexed(Index<Node> index, String field, Object value, SearchParameter params);

  T findIndexedSingle(String field, Object value);

  T findIndexedSingle(String field, Object value, SearchParameter params);

  T findIndexedSingle(Index<Node> index, String field, Object value);

  T findIndexedSingle(Index<Node> index, String field, Object value, SearchParameter params);

  Class<?> getModelClass();

  Node getNode(Model model);
  
  /**
   * Get the database instance associated with the controller
   *
   * @return Database instance
   */
  GraphDatabaseService graph();

  Index<Node> index();

  Index<Node> index(String indexName);

  void index(T model, String name, Object value);

  void index(Index<Node> index, T model, String name, Object value);

  Map<String, String> indexConfig(String index);

  Index<Node> indexForField(String fieldName);

  void indexRemove(Index<Node> index, Node node);

  void indexRemove(Node node, String field);

  void indexRemove(Node node);

  void indexRemove(Index<Node> index, Node node, String field);

  /**
   * Get the main label
   *
   * @return Main label
   */
  Label label();

  Set<Label> labels();

  QueryBuilder queryBuilder();

  String queryIdentifier();

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
